package ru.aav;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.IllegalTransactionStateException;

import ru.aav.account.*;
import ru.aav.cart.*;
import ru.aav.order.*;
import ru.aav.repository.ProductRepository;
import ru.aav.service.ProductService;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.UUID;

import javax.imageio.ImageIO;

/**
 * Сквозные сценарии с настоящими БД, кодировщиком паролей, CSRF и проверками владельца.
 * {@code @SpringBootTest} поднимает контекст приложения для интеграционной проверки.
 * {@code @AutoConfigureMockMvc} настраивает MockMvc вместе с фильтрами безопасности.
 * {@code @ActiveProfiles} включает изолированные настройки тестовой базы.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerShoppingTests {
    /**
     * Логин первого тестового пользователя. Логин второго тестового пользователя для проверки
     * изоляции. Пароль только для тестовых аккаунтов.
     */
    private static final String ALICE = "alice@example.com",
            BOB = "bob@example.com",
            PASS = "long-password-123";

    /**
     * MockMvc выполняет HTTP-запросы к Spring MVC без запуска отдельного браузера.
     * {@code @Autowired} внедряет готовую зависимость Spring в тест.
     */
    @Autowired MockMvc mvc;

    /**
     * Зависимость для поиска и изменения учётных записей. {@code @Autowired} внедряет готовую
     * зависимость Spring в тест.
     */
    @Autowired AccountService accounts;

    /**
     * Репозиторий аккаунтов для операций хранения и блокировки владельца. {@code @Autowired}
     * внедряет готовую зависимость Spring в тест.
     */
    @Autowired AccountRepository accountRepository;

    /**
     * Зависимость для операций с личной корзиной. {@code @Autowired} внедряет готовую зависимость
     * Spring в тест.
     */
    @Autowired CartService carts;

    /**
     * Репозиторий позиций корзины для подготовки и проверки тестовых данных. {@code @Autowired}
     * внедряет готовую зависимость Spring в тест.
     */
    @Autowired CartRepository cartRepository;

    /**
     * Зависимость для работы с заказами. {@code @Autowired} внедряет готовую зависимость Spring в
     * тест.
     */
    @Autowired OrderService orders;

    /**
     * Репозиторий заказов для проверки результатов сохранения. {@code @Autowired} внедряет готовую
     * зависимость Spring в тест.
     */
    @Autowired OrderRepository orderRepository;

    /**
     * Зависимость для доступа к продуктам. {@code @Autowired} внедряет готовую зависимость Spring в
     * тест.
     */
    @Autowired ProductService products;

    /**
     * Репозиторий продуктов для подготовки тестов и проверки состояния базы. {@code @Autowired}
     * внедряет готовую зависимость Spring в тест.
     */
    @Autowired ProductRepository productRepository;

    /**
     * Кодировщик паролей для проверки сохранённых хешей. {@code @Autowired} внедряет готовую
     * зависимость Spring в тест.
     */
    @Autowired PasswordEncoder encoder;

    /**
     * Сервис проверки и преобразования загруженных фото. {@code @Autowired} внедряет готовую
     * зависимость Spring в тест.
     */
    @Autowired AvatarService avatars;

    /** Идентификатор продукта; в истории это снимок, а не внешняя ссылка на живой товар. */
    int productId;

    /**
     * Очищает заказы, корзины и аккаунты и создаёт двух независимых пользователей с тестовым
     * продуктом. {@code @BeforeEach} подготавливает чистые данные перед каждым тестом.
     */
    @BeforeEach
    void reset() {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        accountRepository.deleteAll();
        accounts.register(new AccountForms.Registration("Алиса", ALICE, PASS, PASS), null);
        accounts.register(new AccountForms.Registration("Борис", BOB, PASS, PASS), null);
        productId = products.createProduct("Томаты", new BigDecimal("119.90")).id();
    }

    /**
     * Проверяет открытые страницы, защиту личных маршрутов и обязательность CSRF. {@code @Test}
     * отмечает независимый сценарий JUnit.
     */
    @Test
    void publicCatalogAndPrivateRoutesHaveCorrectAccess() throws Exception {
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(containsString("Войдите, чтобы составить свой список")));
        mvc.perform(get("/account.css")).andExpect(status().isOk());
        mvc.perform(get("/account.js")).andExpect(status().isOk());
        for (String path : new String[] {"/account", "/cart", "/orders", "/account/avatar"})
            mvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        mvc.perform(post("/cart/add").with(user(ALICE)).param("productId", "" + productId))
                .andExpect(status().isForbidden());
        assertEquals(0, carts.view(ALICE).quantity());
    }

    /**
     * Проверяет нормализацию email, хеширование пароля, уникальность и настоящий вход.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void registersCanonicalEmailAndLogsInWithRealPassword() throws Exception {
        mvc.perform(
                        multipart("/register")
                                .with(csrf())
                                .param("displayName", "Андрей")
                                .param("email", "NEW@Example.com")
                                .param("password", PASS)
                                .param("confirmation", PASS))
                .andExpect(redirectedUrl("/login"));
        var account = accountRepository.findByEmail("new@example.com").orElseThrow();
        assertNotEquals(PASS, account.getPasswordHash());
        assertTrue(encoder.matches(PASS, account.getPasswordHash()));
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", "NEW@Example.com")
                                .param("password", PASS))
                .andExpect(authenticated().withUsername("new@example.com"))
                .andExpect(redirectedUrl("/account"));
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", ALICE)
                                .param("password", "incorrect"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(
                        multipart("/register")
                                .with(csrf())
                                .param("displayName", "Дубликат")
                                .param("email", "NEW@example.com")
                                .param("password", PASS)
                                .param("confirmation", PASS))
                .andExpect(status().isBadRequest());
        assertEquals(3, accountRepository.count());
    }

    /**
     * Проверяет ошибки регистрации и отсутствие возврата пароля в HTML. {@code @Test} отмечает
     * независимый сценарий JUnit.
     */
    @Test
    void registrationRejectsBadDataWithoutEchoingPasswords() throws Exception {
        mvc.perform(
                        multipart("/register")
                                .with(csrf())
                                .param("displayName", "")
                                .param("email", "wrong")
                                .param("password", "short")
                                .param("confirmation", "short"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("value=\"short\""))));
        mvc.perform(
                        multipart("/register")
                                .with(csrf())
                                .param("displayName", "Имя")
                                .param("email", "new@example.com")
                                .param("password", PASS)
                                .param("confirmation", "different"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Пароли не совпадают")));
        assertEquals(2, accountRepository.count());
    }

    /**
     * Проверяет изменение, удаление и очистку корзины без изменения данных второго пользователя.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void updatesRemovesAndClearsOnlyOwnCart() throws Exception {
        carts.add(BOB, productId, 4);
        mvc.perform(
                        post("/cart/add")
                                .with(user(ALICE))
                                .with(csrf())
                                .param("productId", "" + productId)
                                .param("quantity", "2"))
                .andExpect(status().is3xxRedirection());
        assertEquals(new BigDecimal("239.80"), carts.view(ALICE).total());
        mvc.perform(get("/cart").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Томаты")));
        mvc.perform(
                        post("/cart/update")
                                .with(user(ALICE))
                                .with(csrf())
                                .param("productId", "" + productId)
                                .param("quantity", "3"))
                .andExpect(redirectedUrl("/cart"));
        assertEquals(3, carts.view(ALICE).quantity());
        for (String qty : new String[] {"0", "100", "-1"})
            mvc.perform(
                            post("/cart/update")
                                    .with(user(ALICE))
                                    .with(csrf())
                                    .param("productId", "" + productId)
                                    .param("quantity", qty))
                    .andExpect(redirectedUrl("/cart"));
        assertEquals(3, carts.view(ALICE).quantity());
        mvc.perform(
                        post("/cart/remove")
                                .with(user(ALICE))
                                .with(csrf())
                                .param("productId", "" + productId))
                .andExpect(redirectedUrl("/cart"));
        assertEquals(0, carts.view(ALICE).quantity());
        assertEquals(4, carts.view(BOB).quantity());
        carts.add(ALICE, productId, 2);
        mvc.perform(post("/cart/clear").with(user(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/cart"));
        assertEquals(0, carts.view(ALICE).quantity());
        assertEquals(4, carts.view(BOB).quantity());
    }

    /**
     * Проверяет отсутствие дубликата при повторе оформления и сохранность исходных цен в истории.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void checkoutIsIdempotentAndHistoryPreservesOriginalPrices() throws Exception {
        carts.add(ALICE, productId, 2);
        carts.add(BOB, productId, 1);
        var view =
                mvc.perform(get("/cart").with(user(ALICE))).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) view.getRequest().getSession(false);
        var form = (CheckoutForm) view.getModelAndView().getModel().get("checkout");
        String location =
                mvc.perform(
                                post("/checkout")
                                        .session(session)
                                        .with(user(ALICE))
                                        .with(csrf())
                                        .param("recipient", "Алиса")
                                        .param("phone", "+79991234567")
                                        .param("address", "Москва, дом 1")
                                        .param("comment", "Без звонка")
                                        .param("token", form.token()))
                        .andExpect(status().is3xxRedirection())
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl();
        mvc.perform(
                        post("/checkout")
                                .session(session)
                                .with(user(ALICE))
                                .with(csrf())
                                .param("recipient", "Алиса")
                                .param("phone", "+79991234567")
                                .param("address", "Москва, дом 1")
                                .param("comment", "Без звонка")
                                .param("token", form.token()))
                .andExpect(redirectedUrl(location));
        assertEquals(1, orderRepository.count());
        assertEquals(0, carts.view(ALICE).quantity());
        assertEquals(1, carts.view(BOB).quantity());
        var order = orders.history(ALICE, 0).getContent().getFirst();
        assertEquals(new BigDecimal("239.80"), order.total());
        products.updateProduct(productId, "Томаты новые", new BigDecimal("199.00"));
        assertEquals("Томаты", orders.get(ALICE, order.id()).lines().getFirst().name());
        assertEquals(new BigDecimal("239.80"), orders.get(ALICE, order.id()).total());
        mvc.perform(get(location).with(user(ALICE))).andExpect(status().isOk());
        mvc.perform(get(location).with(user(BOB))).andExpect(status().isNotFound());
        mvc.perform(post(location + "/repeat").with(user(BOB)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/orders").with(user(ALICE))).andExpect(status().isOk());
        mvc.perform(post(location + "/repeat").with(user(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/cart"));
        assertEquals(new BigDecimal("398.00"), carts.view(ALICE).total());
        assertEquals(0, orders.history(BOB, 0).getTotalElements());
    }

    /**
     * Проверяет отказ для пустой корзины и токена, не принадлежащего сессии. {@code @Test} отмечает
     * независимый сценарий JUnit.
     */
    @Test
    void emptyCheckoutAndInvalidTokenDoNotCreateOrders() throws Exception {
        var view = mvc.perform(get("/cart").with(user(ALICE))).andReturn();
        var session = (MockHttpSession) view.getRequest().getSession(false);
        var form = (CheckoutForm) view.getModelAndView().getModel().get("checkout");
        mvc.perform(
                        post("/checkout")
                                .session(session)
                                .with(user(ALICE))
                                .with(csrf())
                                .param("recipient", "Алиса")
                                .param("phone", "123")
                                .param("address", "Адрес")
                                .param("comment", "")
                                .param("token", form.token()))
                .andExpect(status().isBadRequest());
        carts.add(ALICE, productId, 1);
        mvc.perform(
                        post("/checkout")
                                .session(session)
                                .with(user(ALICE))
                                .with(csrf())
                                .param("recipient", "Алиса")
                                .param("phone", "123")
                                .param("address", "Адрес")
                                .param("comment", "")
                                .param("token", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
        assertEquals(0, orderRepository.count());
        assertEquals(1, carts.view(ALICE).quantity());
    }

    /**
     * Проверяет профиль, загрузку и удаление фото, смену пароля и изоляцию пользователей.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void profileAvatarAndPasswordChangeWorkWithoutExposingOtherUsers() throws Exception {
        mvc.perform(get("/account").with(user(ALICE))).andExpect(status().isOk());
        mvc.perform(
                        post("/account/profile")
                                .with(user(ALICE))
                                .with(csrf())
                                .param("displayName", "Алиса Новая")
                                .param("phone", "+7 999")
                                .param("address", "Москва, улица 1"))
                .andExpect(redirectedUrl("/account"));
        assertEquals("Алиса Новая", accounts.view(ALICE).displayName());
        assertEquals("", accounts.view(BOB).address());
        var image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", bytes);
        mvc.perform(
                        multipart("/account/avatar")
                                .file(
                                        new MockMultipartFile(
                                                "avatar",
                                                "photo.png",
                                                "image/png",
                                                bytes.toByteArray()))
                                .with(user(ALICE))
                                .with(csrf()))
                .andExpect(redirectedUrl("/account#photo"));
        mvc.perform(get("/account/avatar").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        mvc.perform(get("/account/avatar").with(user(BOB))).andExpect(status().isNotFound());
        mvc.perform(
                        multipart("/account/avatar")
                                .file(
                                        new MockMultipartFile(
                                                "avatar",
                                                "fake.png",
                                                "image/png",
                                                "<script>bad()</script>".getBytes()))
                                .with(user(ALICE))
                                .with(csrf()))
                .andExpect(flash().attributeExists("formError"));
        mvc.perform(post("/account/avatar/remove").with(user(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/account#photo"));
        assertFalse(accounts.view(ALICE).hasAvatar());
        mvc.perform(
                        post("/account/password")
                                .with(user(ALICE))
                                .with(csrf())
                                .param("currentPassword", "wrong")
                                .param("newPassword", "new-long-password")
                                .param("confirmation", "new-long-password"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/account/password")
                                .with(user(ALICE))
                                .with(csrf())
                                .param("currentPassword", PASS)
                                .param("newPassword", "new-long-password")
                                .param("confirmation", "new-long-password"))
                .andExpect(redirectedUrl("/login?passwordChanged"))
                .andExpect(unauthenticated());
        mvc.perform(post("/login").with(csrf()).param("email", ALICE).param("password", PASS))
                .andExpect(unauthenticated());
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", ALICE)
                                .param("password", "new-long-password"))
                .andExpect(authenticated());
    }

    /**
     * Проверяет лимит фото, неподдерживаемый формат и границы количества в корзине. {@code @Test}
     * отмечает независимый сценарий JUnit.
     */
    @Test
    void rejectsOversizedAndInvalidImageContent() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        avatars.normalize(
                                new MockMultipartFile("avatar", new byte[2 * 1024 * 1024 + 1])));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        avatars.normalize(
                                new MockMultipartFile(
                                        "avatar",
                                        "image.svg",
                                        "image/svg+xml",
                                        "<svg/>".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> carts.add(ALICE, productId, 100));
        carts.add(ALICE, productId, 99);
        assertThrows(IllegalArgumentException.class, () -> carts.add(ALICE, productId, 1));
        assertEquals(99, carts.view(ALICE).quantity());
    }

    /**
     * Проверяет ручной ввод продукта и запрет доступа к чужому личному списку. {@code @Test}
     * отмечает независимый сценарий JUnit.
     */
    @Test
    void manualEntryAddsDirectlyToOwnCartAndHidesLegacyCatalog() throws Exception {
        products.createProduct("Война и мир", new BigDecimal("500.00"));
        mvc.perform(
                        post("/products")
                                .with(user(ALICE))
                                .with(csrf())
                                .param("name", "Молоко")
                                .param("price", "89.90"))
                .andExpect(redirectedUrl("/cart"));
        assertEquals(1, carts.view(ALICE).quantity());
        assertEquals(new BigDecimal("89.90"), carts.view(ALICE).total());
        assertEquals(0, carts.view(BOB).quantity());
        mvc.perform(get("/products").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Молоко")))
                .andExpect(content().string(not(containsString("Война и мир"))));
        mvc.perform(get("/products").with(user(BOB)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Молоко"))))
                .andExpect(content().string(not(containsString("Война и мир"))));
        var personal =
                products.getPersonalProducts(accounts.view(ALICE).id(), 0).getContent().getFirst();
        mvc.perform(
                        post("/cart/add")
                                .with(user(BOB))
                                .with(csrf())
                                .param("productId", "" + personal.id()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/products/" + personal.id() + "/remove").with(user(BOB)).with(csrf()))
                .andExpect(status().isNotFound());
        assertEquals(1, carts.view(ALICE).quantity());
    }

    /**
     * Проверяет удаление и полную очистку списка, включая продукты за первой страницей.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void removesPersonalProductAndClearsOnlyOwnListAcrossPages() throws Exception {
        long alice = accounts.view(ALICE).id(), bob = accounts.view(BOB).id();
        var first = products.createInCart(alice, "Молоко", new BigDecimal("89.90"));
        products.createInCart(bob, "Хлеб Бориса", new BigDecimal("70.00"));
        mvc.perform(post("/products/" + first.id() + "/remove").with(user(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/products#catalog"));
        assertTrue(products.getPersonalProducts(alice, 0).isEmpty());
        assertEquals(0, carts.view(ALICE).quantity());
        for (int i = 0; i < 21; i++) products.createInCart(alice, "Продукт " + i, BigDecimal.ONE);
        assertEquals(21, products.getPersonalProducts(alice, 0).getTotalElements());
        mvc.perform(post("/products/clear").with(user(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/products#catalog"));
        assertEquals(0, products.getPersonalProducts(alice, 0).getTotalElements());
        assertEquals(0, carts.view(ALICE).quantity());
        assertEquals(1, products.getPersonalProducts(bob, 0).getTotalElements());
        assertEquals(1, carts.view(BOB).quantity());
    }

    /**
     * Проверяет сохранность истории после удаления продуктов и отказ повторить недоступный состав.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void clearingListPreservesPreviouslyPlacedOrderSnapshots() {
        long alice = accounts.view(ALICE).id();
        var product = products.createInCart(alice, "Яблоки", new BigDecimal("120.00"));
        long order =
                orders.checkout(
                        ALICE,
                        new CheckoutForm(
                                "Алиса", "123", "Адрес", "", UUID.randomUUID().toString()));
        carts.add(ALICE, product.id(), 1);
        products.clearPersonal(alice);
        assertEquals(0, carts.view(ALICE).quantity());
        assertEquals("Яблоки", orders.get(ALICE, order).lines().getFirst().name());
        assertEquals(new BigDecimal("120.00"), orders.get(ALICE, order).total());
        assertThrows(IllegalArgumentException.class, () -> orders.repeat(ALICE, order));
    }

    /** Проверяет агрегат для шапки: пустая корзина, сумма количеств и изоляция владельцев. */
    @Test
    void headerQuantityUsesOnlyCurrentOwnersCart() {
        assertEquals(0L, carts.quantity(ALICE));
        carts.add(ALICE, productId, 3);
        carts.add(BOB, productId, 7);
        assertEquals(3L, carts.quantity(ALICE));
        assertEquals(7L, carts.quantity(BOB));
        carts.clear(ALICE);
        assertEquals(0L, carts.quantity(ALICE));
    }

    /** Проверяет валидацию даже при обращении к сервису без MVC-контроллера. */
    @Test
    void serviceValidationRejectsInvalidProfileAndRegistration() {
        assertThrows(
                ConstraintViolationException.class,
                () -> accounts.update(ALICE, new AccountForms.Profile(" ", "", "")));
        assertThrows(
                ConstraintViolationException.class,
                () ->
                        accounts.register(
                                new AccountForms.Registration("", "bad", "123", "123"), null));
        assertEquals("Алиса", accounts.view(ALICE).displayName());
        assertEquals(2, accountRepository.count());
    }

    /** Проверяет, что прямой вызов оформления также валидирует контакты и не очищает корзину. */
    @Test
    void serviceValidationRejectsInvalidCheckout() {
        carts.add(ALICE, productId, 1);
        assertThrows(
                ConstraintViolationException.class,
                () -> orders.checkout(ALICE, new CheckoutForm("", "", "", "", "bad")));
        assertEquals(1, carts.view(ALICE).quantity());
        assertEquals(0, orderRepository.count());
    }

    /** Запрос блокировки вне транзакции запрещён: иначе блокировка не защищала бы всю операцию. */
    @Test
    void accountLockRequiresExistingTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> carts.lock(ALICE));
    }

    /**
     * Повтор заказа с переполнением количества откатывает и предыдущие добавления в том же запросе.
     */
    @Test
    void repeatingOrderRollsBackAllLinesOnQuantityOverflow() {
        long ownerId = accounts.view(ALICE).id();
        products.createInCart(ownerId, "Первая позиция", BigDecimal.ONE);
        var second = products.createInCart(ownerId, "Вторая позиция", BigDecimal.TEN);
        long orderId =
                orders.checkout(
                        ALICE,
                        new CheckoutForm(
                                "Алиса", "123", "Адрес", "", UUID.randomUUID().toString()));
        carts.add(ALICE, second.id(), 99);
        assertThrows(IllegalArgumentException.class, () -> orders.repeat(ALICE, orderId));
        assertEquals(1, carts.view(ALICE).lines().size());
        assertEquals(99, carts.view(ALICE).quantity());
    }

    /**
     * После удаления дерева его URL больше не отдаёт изображение; нужная иллюстрация магазина
     * доступна.
     */
    @Test
    void obsoleteTreeImageIsAbsentButGroceryIllustrationExists() throws Exception {
        mvc.perform(get("/images/1.png")).andExpect(status().isNotFound());
        mvc.perform(get("/images/grocery-hero.webp")).andExpect(status().isOk());
    }
}
