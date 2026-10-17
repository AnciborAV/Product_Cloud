package ru.aav;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.aav.model.Product;
import ru.aav.repository.ProductRepository;
import ru.aav.service.ProductNotFoundException;
import ru.aav.service.ProductService;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCloudJpaApplicationTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    ProductRepository repository;
    @Autowired
    ProductService service;

    @BeforeEach
    void clearProducts() {
        repository.deleteAll();
    }

    @Test
    void rendersEmptyCatalogAndRedirectsHome() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/products"));
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Здесь пока нет товаров")));
        mvc.perform(get("/styles.css")).andExpect(status().isOk());
    }

    @Test
    void createsProductWithExactPriceAndRedirects() throws Exception {
        mvc.perform(post("/products").param("name", "  Книга  ").param("price", "19.90"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products"));
        var product = repository.findAll().getFirst();
        assertEquals("Книга", product.getName());
        assertEquals(new BigDecimal("19.90"), product.getPrice());
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Книга")));
        assertEquals(1, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "1.001", "abc", "", "100000000000000000.00"})
    void rejectsInvalidPricesWithoutWriting(String price) throws Exception {
        mvc.perform(post("/products").param("name", "Book").param("price", price))
                .andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("productForm", "price"));
        assertEquals(0, repository.count());
    }

    @Test
    void rejectsMissingBlankAndOversizedNames() throws Exception {
        for (String name : new String[]{"", "   ", "x".repeat(256)}) {
            mvc.perform(post("/products").param("name", name).param("price", "1"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/products").param("price", "1"))
                .andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }

    @Test
    void zeroPriceIsAllowedAndClientIdCannotOverwriteExistingProduct() throws Exception {
        var original = service.createProduct("Original", BigDecimal.ONE);
        mvc.perform(post("/products").param("id", Integer.toString(original.id()))
                        .param("name", "New").param("price", "0.00"))
                .andExpect(status().is3xxRedirection());
        assertEquals(2, repository.count());
        assertEquals("Original", service.getProductById(original.id()).name());
    }

    @Test
    void escapesHtmlInProductNames() throws Exception {
        service.createProduct("<script>alert(1)</script>", BigDecimal.ONE);
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

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

    @Test
    void missingProductsProduceExplicitErrors() {
        assertThrows(ProductNotFoundException.class, () -> service.getProductById(Integer.MAX_VALUE));
        assertThrows(ProductNotFoundException.class,
                () -> service.updateProduct(Integer.MAX_VALUE, "Missing", BigDecimal.ONE));
        assertThrows(ProductNotFoundException.class, () -> service.deleteProduct(Integer.MAX_VALUE));
    }

    @Test
    void domainValidationPreventsInvalidServiceWrites() {
        assertThrows(IllegalArgumentException.class, () -> service.createProduct(" ", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new Product("Valid", new BigDecimal("0.001")));
        var created = service.createProduct("Original", BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class,
                () -> service.updateProduct(created.id(), "Changed", new BigDecimal("-1")));
        assertEquals("Original", service.getProductById(created.id()).name());
    }

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
