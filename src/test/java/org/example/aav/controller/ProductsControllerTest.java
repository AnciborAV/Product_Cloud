package org.example.aav.controller;

import org.example.aav.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ProductsControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired ProductService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test
    void rendersStorefrontAtRoot() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("products"))
                .andExpect(content().string(containsString("зелёная")))
                .andExpect(content().string(containsString("href=\"/css/store.css?v=2\"")))
                .andExpect(content().string(containsString("Авокадо Хасс")));
    }

    @Test
    void filtersProducts() throws Exception {
        mvc.perform(get("/products").param("q", "МОЛОКО").param("category", "DAIRY"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Молоко фермерское")))
                .andExpect(content().string(not(containsString("Авокадо Хасс"))));
    }

    @Test
    void displaysEmptyState() throws Exception {
        mvc.perform(get("/products").param("q", "несуществующийтовар"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Пока ничего не нашлось")));
    }

    @Test
    void addsProductAndRedirectsToPreventDuplicateSubmission() throws Exception {
        int before = service.findAll().size();
        mvc.perform(post("/products").param("name", "Тестовая груша").param("price", "123.45")
                        .param("unit", "1 кг").param("category", "VEGETABLES"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products"))
                .andExpect(flash().attributeExists("message"));
        assertThat(service.findAll()).hasSize(before + 1);
    }

    @Test
    void invalidFormDoesNotChangeCatalog() throws Exception {
        int before = service.findAll().size();
        mvc.perform(post("/products").param("name", " ").param("price", "-1").param("category", ""))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("productForm", "name", "price", "category"))
                .andExpect(content().string(containsString("Проверьте данные товара")));
        assertThat(service.findAll()).hasSize(before);
    }

    @Test
    void handlesMalformedPriceAsFormError() throws Exception {
        mvc.perform(post("/products").param("name", "Хлеб").param("price", "abc").param("category", "BAKERY"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("productForm", "price"));
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        mvc.perform(get("/products").param("category", "UNKNOWN")).andExpect(status().isBadRequest());
    }

    @Test
    void escapesUserSuppliedProductName() throws Exception {
        mvc.perform(post("/products").param("name", "<script>alert(1)</script>").param("price", "10").param("category", "PANTRY"));
        mvc.perform(get("/products").param("q", "<script>"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    void servesStylesheet() throws Exception {
        mvc.perform(get("/css/store.css").param("v", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString(".product-grid")))
                .andExpect(content().string(containsString("grid-template-columns:repeat(4,minmax(0,1fr))")));
    }
}
