package org.example.aav.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.example.aav.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Этап 8: добавляем проверки витрины и запросов к каталогу, чтобы замечать ошибки при изменениях. Учебная последовательность. */
@SpringBootTest
class ProductsControllerTest {
  // Контекст Spring: настройки и объекты приложения.
  @Autowired WebApplicationContext context;
  // Сервис товаров, работу которого проверяет тест.
  @Autowired ProductService service;
  // Имитация запросов браузера для проверки контроллеров.
  MockMvc mvc;

  /**
   * Перед каждым тестом создаёт имитацию браузера с защитой Spring Security.
   */
  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  /**
   * Проверяет показ витрины по главному адресу сайта.
   */
  @Test
  void rendersStorefrontAtRoot() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("products"))
        .andExpect(content().string(containsString("зелёная")))
        .andExpect(content().string(containsString("href=\"/css/store.css?v=2\"")))
        .andExpect(content().string(containsString("Авокадо Хасс")));
  }

  /**
   * Проверяет совместную работу поиска и фильтра категории.
   */
  @Test
  void filtersProducts() throws Exception {
    mvc.perform(get("/products").param("q", "МОЛОКО").param("category", "DAIRY"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Молоко фермерское")))
        .andExpect(content().string(not(containsString("Авокадо Хасс"))));
  }

  /**
   * Проверяет сообщение при отсутствии найденных товаров.
   */
  @Test
  void displaysEmptyState() throws Exception {
    mvc.perform(get("/products").param("q", "несуществующийтовар"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Пока ничего не нашлось")));
  }

  /**
   * Проверяет добавление товара и перенаправление после отправки формы.
   */
  @Test
  void addsProductAndRedirectsToPreventDuplicateSubmission() throws Exception {
    // Число товаров до действия для сравнения с результатом.
    int before = service.findAll().size();
    mvc.perform(
            post("/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", "Тестовая груша")
                .param("price", "123.45")
                .param("unit", "1 кг")
                .param("category", "VEGETABLES"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/products"))
        .andExpect(flash().attributeExists("message"));
    assertThat(service.findAll()).hasSize(before + 1);
  }

  /**
   * Проверяет сохранность каталога при ошибках формы.
   */
  @Test
  void invalidFormDoesNotChangeCatalog() throws Exception {
    // Число товаров до действия для сравнения с результатом.
    int before = service.findAll().size();
    mvc.perform(
            post("/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", " ")
                .param("price", "-1")
                .param("category", ""))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("productForm", "name", "price", "category"))
        .andExpect(content().string(containsString("Проверьте данные товара")));
    assertThat(service.findAll()).hasSize(before);
  }

  /**
   * Проверяет понятную ошибку формы при цене, введённой буквами.
   */
  @Test
  void handlesMalformedPriceAsFormError() throws Exception {
    mvc.perform(
            post("/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", "Хлеб")
                .param("price", "abc")
                .param("category", "BAKERY"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("productForm", "price"));
  }

  /**
   * Проверяет отказ при неизвестной категории.
   */
  @Test
  void rejectsUnknownCategory() throws Exception {
    mvc.perform(get("/products").param("category", "UNKNOWN")).andExpect(status().isBadRequest());
  }

  /**
   * Проверяет безопасный вывод названия с HTML-символами.
   */
  @Test
  void escapesUserSuppliedProductName() throws Exception {
    mvc.perform(
        post("/products")
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .param("name", "<script>alert(1)</script>")
            .param("price", "10")
            .param("category", "PANTRY"));
    mvc.perform(get("/products").param("q", "<script>"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("&lt;script&gt;")))
        .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
  }

  /**
   * Проверяет выдачу файла стилей витрины.
   */
  @Test
  void servesStylesheet() throws Exception {
    mvc.perform(get("/css/store.css").param("v", "2"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/css"))
        .andExpect(content().string(containsString(".product-grid")))
        .andExpect(
            content().string(containsString("grid-template-columns: repeat(4, minmax(0, 1fr))")));
  }
}
