package ru.aav.bookshop;

import com.sun.net.httpserver.HttpServer;
import ru.aav.bookshop.controller.*;
import ru.aav.bookshop.security.AuthService;
import ru.aav.bookshop.service.LibraryService;
import ru.aav.bookshop.repository.*;
import ru.aav.bookshop.service.ShopService;
import ru.aav.bookshop.view.ShopView;
import java.net.InetSocketAddress;

import java.util.concurrent.Executors;

/** Точка входа и сборка зависимостей вручную: здесь хорошо видно, кто от кого зависит. */
public final class BookshopApplication {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("shop.port", "8081"));
        Database database = Database.fromEnvironment();
        database.initialize();
        var service = new ShopService(new BookRepository(database), new OrderRepository(database));
        // Пример предназначен для локального обучения и слушает только loopback-интерфейс.
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 32);
        var executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        var view = new ShopView();
        var library = new LibraryService(new LibraryRepository(database));
        server.createContext("/", new AuthController(new AuthService(database),
            new ShopController(service, view), new LibraryController(library, view), view));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { server.stop(1); executor.shutdown(); }));
        server.start();
        System.out.println("Тихая полка: http://localhost:" + port);
        System.out.println("Каталог и заказы хранятся в PostgreSQL.");
    }
}
