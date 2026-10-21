package ru.aav;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ru.aav.account.*;
import ru.aav.cart.CartRepository;
import ru.aav.model.Product;
import ru.aav.order.OrderRepository;
import ru.aav.repository.ProductRepository;
import ru.aav.service.ProductNotFoundException;
import ru.aav.service.ProductService;

import java.math.BigDecimal;

/**
 * Интеграционные тесты запускают полный контекст Spring Boot. MockMvc вызывает MVC-обработчики и
 * рендеринг шаблонов без отдельного HTTP-сервера. Профиль test выбирает отдельную H2-базу локально
 * или PostgreSQL в CI. @Autowired внедряет тестовые зависимости; сервис/репозиторий здесь
 * настоящие. Тесты не обёрнуты общей транзакцией: проверяется реальное завершение транзакций
 * сервиса. {@code @SpringBootTest} поднимает контекст приложения для интеграционной проверки.
 * {@code @AutoConfigureMockMvc} настраивает MockMvc вместе с фильтрами безопасности.
 * {@code @ActiveProfiles} включает изолированные настройки тестовой базы.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "catalog-test@example.com")
class ProductCloudJpaApplicationTests {
    /**
     * MockMvc выполняет HTTP-запросы к Spring MVC без запуска отдельного браузера.
     * {@code @Autowired} внедряет готовую зависимость Spring в тест.
     */
    @Autowired MockMvc mvc;

    /**
     * Репозиторий, изолирующий хранение данных от вызывающего слоя. {@code @Autowired} внедряет
     * готовую зависимость Spring в тест.
     */
    @Autowired ProductRepository repository;

    /**
     * Сервис бизнес-операций, отделённый от HTTP. {@code @Autowired} внедряет готовую зависимость
     * Spring в тест.
     */
    @Autowired ProductService service;

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
     * Репозиторий позиций корзины для подготовки и проверки тестовых данных. {@code @Autowired}
     * внедряет готовую зависимость Spring в тест.
     */
    @Autowired CartRepository cartRepository;

    /**
     * Репозиторий заказов для проверки результатов сохранения. {@code @Autowired} внедряет готовую
     * зависимость Spring в тест.
     */
    @Autowired OrderRepository orderRepository;

    /**
     * Очищает таблицу перед каждым тестом, чтобы результаты не зависели от порядка запуска.
     * {@code @BeforeEach} подготавливает чистые данные перед каждым тестом.
     */
    @BeforeEach
    void clearProducts() {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        repository.deleteAll();
        accountRepository.deleteAll();
        accounts.register(
                new AccountForms.Registration(
                        "Тест каталога",
                        "catalog-test@example.com",
                        "test-password-123",
                        "test-password-123"),
                null);
    }

    /**
     * Проверяет маршрут главной страницы, пустой каталог и доступность CSS. {@code @Test} отмечает
     * независимый сценарий JUnit.
     */
    @Test
    void rendersEmptyCatalogAndRedirectsHome() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/products"));
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ваш список пока пуст")));
        mvc.perform(get("/styles.css")).andExpect(status().isOk());
    }

    /**
     * Проверяет создание через HTTP-форму, удаление пробелов, точность цены и перенаправление.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void createsProductWithExactPriceAndRedirects() throws Exception {
        mvc.perform(
                        post("/products")
                                .with(csrf())
                                .param("name", "  Книга  ")
                                .param("price", "19.90"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
        var product = repository.findAll().getFirst();
        assertEquals("Книга", product.getName());
        assertEquals(new BigDecimal("19.90"), product.getPrice());
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Книга")));
        assertEquals(1, repository.count());
    }

    /**
     * Каждое значение из ValueSource — отдельный запуск: неверная цена не должна попасть в БД.
     * {@code @ParameterizedTest} запускает сценарий для каждого набора аргументов.
     * {@code @ValueSource} перечисляет входные значения параметризованного теста.
     */
    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "1.001", "abc", "", "100000000000000000.00"})
    void rejectsInvalidPricesWithoutWriting(String price) throws Exception {
        mvc.perform(post("/products").with(csrf()).param("name", "Book").param("price", price))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeHasFieldErrors("productForm", "price"));
        assertEquals(0, repository.count());
    }

    /**
     * Проверяет отсутствие названия, пробелы и превышение ограничения длины. {@code @Test} отмечает
     * независимый сценарий JUnit.
     */
    @Test
    void rejectsMissingBlankAndOversizedNames() throws Exception {
        for (String name : new String[] {"", "   ", "x".repeat(256)}) {
            mvc.perform(post("/products").with(csrf()).param("name", name).param("price", "1"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/products").with(csrf()).param("price", "1"))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }

    /**
     * Подтверждает допустимость нулевой цены и невозможность перезаписать товар переданным id.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void zeroPriceIsAllowedAndClientIdCannotOverwriteExistingProduct() throws Exception {
        var original = service.createProduct("Original", BigDecimal.ONE);
        mvc.perform(
                        post("/products")
                                .with(csrf())
                                .param("id", Integer.toString(original.id()))
                                .param("name", "New")
                                .param("price", "0.00"))
                .andExpect(status().is3xxRedirection());
        assertEquals(2, repository.count());
        assertEquals("Original", service.getProductById(original.id()).name());
    }

    /**
     * Проверяет экранирование текста Thymeleaf: название не должно превращаться в исполняемый HTML.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void escapesHtmlInProductNames() throws Exception {
        service.createInCart(
                accounts.view("catalog-test@example.com").id(),
                "<script>alert(1)</script>",
                BigDecimal.ONE);
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    /**
     * Проверяет сохранение обновления после завершения транзакции и последующее удаление.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void updatesAndDeletesPersistedProducts() {
        var created = service.createProduct("Before", new BigDecimal("0.10"));
        service.updateProduct(created.id(), "After", new BigDecimal("0.20"));
        var updated = service.getProductById(created.id());
        assertEquals("After", updated.name());
        assertEquals(new BigDecimal("0.20"), updated.price());
        service.deleteProduct(created.id());
        assertEquals(0, repository.count());
    }

    /**
     * Проверяет единый контракт ошибки для чтения, изменения и удаления отсутствующего товара.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void missingProductsProduceExplicitErrors() {
        assertThrows(
                ProductNotFoundException.class, () -> service.getProductById(Integer.MAX_VALUE));
        assertThrows(
                ProductNotFoundException.class,
                () -> service.updateProduct(Integer.MAX_VALUE, "Missing", BigDecimal.ONE));
        assertThrows(
                ProductNotFoundException.class, () -> service.deleteProduct(Integer.MAX_VALUE));
    }

    /**
     * Проверяет защиту модели при обходе формы и сохранение прежних значений после ошибки.
     * {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void domainValidationPreventsInvalidServiceWrites() {
        assertThrows(
                IllegalArgumentException.class, () -> service.createProduct(" ", BigDecimal.ONE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Product("Valid", new BigDecimal("0.001")));
        var created = service.createProduct("Original", BigDecimal.ONE);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateProduct(created.id(), "Changed", new BigDecimal("-1")));
        assertEquals("Original", service.getProductById(created.id()).name());
    }

    /**
     * Проверяет разбиение 21 товара на две страницы, порядок id и HTTP 400 для отрицательного
     * номера. {@code @Test} отмечает независимый сценарий JUnit.
     */
    @Test
    void paginatesInStableOrderAndRejectsInvalidPage() throws Exception {
        for (int i = 0; i < 21; i++) {
            service.createProduct("Product " + i, BigDecimal.ONE);
        }
        var first = service.getProducts(0);
        var second = service.getProducts(1);
        assertEquals(20, first.getNumberOfElements());
        assertEquals(1, second.getNumberOfElements());
        assertTrue(first.getContent().getLast().id() < second.getContent().getFirst().id());
        mvc.perform(get("/products").param("page", "1")).andExpect(status().isOk());
        mvc.perform(get("/products").param("page", "-1")).andExpect(status().isBadRequest());
    }
}
