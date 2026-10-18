package ru.aav.bookshop;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.aav.bookshop.controller.*;
import ru.aav.bookshop.repository.*;
import ru.aav.bookshop.service.*;
import ru.aav.bookshop.security.AuthService;
import ru.aav.bookshop.model.Order;
import ru.aav.bookshop.view.ShopView;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Только выделенная тестовая БД! Интеграционные тесты очищают её таблицы перед каждым тестом. */
@EnabledIfEnvironmentVariable(named="SHOP_TEST_DB_URL", matches=".+")
class PostgresIntegrationTest {
    private Database database;
    @BeforeEach void prepare() throws Exception {
        String url = System.getenv("SHOP_TEST_DB_URL");
        if (!url.matches("jdbc:postgresql://[^/]+/quiet_shelf_test(?:\\?.*)?"))
            throw new IllegalStateException("Тесты разрешены только в базе quiet_shelf_test");
        database = new Database(url, System.getenv().getOrDefault("SHOP_DB_USER", "postgres"), System.getenv("SHOP_DB_PASSWORD"));
        database.initialize();
        try (var connection = database.connect(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE documents, shelves, order_items, orders, app_user RESTART IDENTITY CASCADE");
        }
    }
    @Test void checkoutSurvivesNewRepositoryAndSchemaInitialization() throws Exception {
        var service = new ShopService(new BookRepository(database), new OrderRepository(database));
        var order = service.checkout(Map.of(1L, 2, 2L, 1));
        assertEquals(new BigDecimal("2420.00"), order.total());
        database.initialize();
        var reloaded = new OrderRepository(database).findById(order.id()).orElseThrow();
        assertEquals(order.total(), reloaded.total()); assertEquals(2, reloaded.lines().size());
        assertThrows(IllegalArgumentException.class, () -> service.checkout(Map.of(99999L, 1)));
    }
    @Test void failingOrderRollsBackHeaderAndItems() throws Exception {
        UUID id = UUID.randomUUID();
        var badOrder = new Order(id, Instant.now(), List.of(new Order.Line(99999, "Missing", BigDecimal.ONE, 1)));
        assertThrows(SQLException.class, () -> new OrderRepository(database).save(badOrder));
        try (var connection = database.connect(); var statement = connection.prepareStatement("SELECT COUNT(*) FROM orders WHERE id=?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) { rows.next(); assertEquals(0, rows.getInt(1)); }
        }
    }
    @Test void libraryPreservesBytesAndCanMoveBetweenShelves() throws Exception {
        var library = new LibraryService(new LibraryRepository(database));
        long first = library.createShelf("Ницше"), second = library.createShelf("Философия");
        assertEquals(first, library.createShelf("Ницше"));
        byte[] pdf = "%PDF-1.4\nexample\n%%EOF".getBytes(StandardCharsets.UTF_8);
        UUID id = library.upload(first,"Книга","Автор","book.pdf",pdf);
        var reopened = new LibraryService(new LibraryRepository(database));
        assertArrayEquals(pdf, reopened.content(id).orElseThrow());
        assertTrue(reopened.move(id, second)); assertEquals(second, reopened.documents().get(0).shelfId());
        assertThrows(IllegalArgumentException.class, () -> library.upload(999, "A", "B", "x.pdf", pdf));
    }
    @Test void accountCannotBeOverwrittenAndSessionsCanBeRevoked() throws Exception {
        var auth = new AuthService(database); auth.setup("reader@example.com", "test-password");
        assertThrows(IllegalArgumentException.class, () -> auth.setup("other@example.com", "different"));
        assertTrue(auth.login("reader@example.com", "bad").isEmpty());
        String token = auth.login("READER@example.com", "test-password").orElseThrow();
        assertTrue(auth.authenticated(token)); auth.logout(token); assertFalse(auth.authenticated(token));
        assertTrue(new AuthService(database).login("reader@example.com", "test-password").isPresent());
    }
    @Test void fullHttpJourneyProtectsPdfAndPersistsCheckout() throws Exception {
        var view = new ShopView();
        var shop = new ShopService(new BookRepository(database), new OrderRepository(database));
        var library = new LibraryService(new LibraryRepository(database));
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new AuthController(new AuthService(database), new ShopController(shop,view), new LibraryController(library,view),view));
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var anonymous = HttpClient.newHttpClient();
            assertEquals(303, anonymous.send(HttpRequest.newBuilder(URI.create(base+"/library")).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
            assertEquals(403, client.send(HttpRequest.newBuilder(URI.create(base+"/setup")).POST(HttpRequest.BodyPublishers.ofString("email=a@b.ru&password=test")).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200, post(client,base+"/setup","email=reader%40example.com&password=test-password").statusCode());
            String catalog = client.send(HttpRequest.newBuilder(URI.create(base+"/")).GET().build(),HttpResponse.BodyHandlers.ofString()).body();
            assertTrue(catalog.contains("Так говорил Заратустра"));
            assertEquals(201, post(client,base+"/api/shelves","name=Test").statusCode());
            byte[] pdf = "%PDF-1.4\nexample\n%%EOF".getBytes(StandardCharsets.UTF_8);
            assertEquals(201, post(client,base+"/api/documents?shelf=1&title=Book&author=Author&filename=book.pdf",new String(pdf,StandardCharsets.UTF_8)).statusCode());
            UUID id = library.documents().get(0).id();
            var fileRequest = HttpRequest.newBuilder(URI.create(base+"/files/"+id)).GET().build();
            assertEquals(303, anonymous.send(fileRequest,HttpResponse.BodyHandlers.ofByteArray()).statusCode());
            var file = client.send(fileRequest,HttpResponse.BodyHandlers.ofByteArray());
            assertEquals("application/pdf",file.headers().firstValue("Content-Type").orElseThrow()); assertArrayEquals(pdf,file.body());
            var download = client.send(HttpRequest.newBuilder(URI.create(base+"/files/"+id+"?download=1")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
            assertTrue(download.headers().firstValue("Content-Disposition").orElseThrow().startsWith("attachment"));
            assertEquals(400, post(client,base+"/orders","1=11").statusCode());
            assertEquals(400, post(client,base+"/orders","1=1&1=2").statusCode());
            var checkout = post(client,base+"/orders","1=2&2=1"); assertEquals(201,checkout.statusCode());
            assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(base+checkout.body())).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200,post(client,base+"/logout","").statusCode());
            assertEquals(303,client.send(fileRequest,HttpResponse.BodyHandlers.ofByteArray()).statusCode());
        } finally { server.stop(0); }
    }
    private HttpResponse<String> post(HttpClient client, String url, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("X-Bookshop","bookshop")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
