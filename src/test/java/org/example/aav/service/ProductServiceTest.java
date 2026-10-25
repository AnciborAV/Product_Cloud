package org.example.aav.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.example.aav.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Этап 8: добавляем проверки поиска и сохранения товаров, чтобы замечать ошибки при изменениях. Учебная последовательность. */
@SpringBootTest
@Transactional
class ProductServiceTest {
  // Сервис товаров, работу которого проверяет тест.
  @Autowired ProductService service;

  /**
   * Проверяет поиск без учёта регистра и пробелов по краям.
   */
  @Test
  void searchesIgnoringCaseAndWhitespace() {
    assertThat(service.search("  МОЛОКО ", null))
        .singleElement()
        // p — очередной товар или пользователь в операции над списком.
        .satisfies(p -> assertThat(p.name()).contains("Молоко"));
  }

  /**
   * Проверяет совместный отбор по строке поиска и категории.
   */
  @Test
  void combinesSearchAndCategory() {
    assertThat(service.search("молоко", Category.BAKERY)).isEmpty();
    assertThat(service.search(null, Category.DAIRY)).hasSize(5);
  }

  /**
   * Проверяет поиск символов % и _ как обычного текста.
   */
  @Test
  void treatsSqlWildcardsLiterally() {
    // p — очередной товар или пользователь в операции над списком.
    assertThat(service.search("%", null)).allSatisfy(p -> assertThat(p.name()).contains("%"));
    assertThat(service.search("_", null)).isEmpty();
  }

  /**
   * Проверяет наличие десяти категорий и пятидесяти читаемых фотографий.
   */
  @Test
  void seededCatalogHasTenDepartmentsAndFiftyLocalPhotos() throws Exception {
    // Товары начального каталога с фотографиями.
    // p — очередной товар или пользователь в операции над списком.
    var catalog = service.findAll().stream().filter(p -> p.getImageUrl() != null).toList();
    assertThat(catalog).hasSize(50);
    // p — очередной товар или пользователь в операции над списком.
    assertThat(catalog.stream().map(p -> p.category()).distinct()).hasSize(10);
    // Товар, с которым выполняется действие.
    for (var product : catalog) {
      // Файл фотографии из ресурсов приложения.
      var resource = new org.springframework.core.io.ClassPathResource("static" + product.getImageUrl());
      assertThat(resource.exists()).as(product.name()).isTrue();
      // Поток чтения изображения; закроется после блока try.
      try (var input = resource.getInputStream()) {
        assertThat(javax.imageio.ImageIO.read(input)).as(product.name()).isNotNull();
      }
    }
  }

  /**
   * Проверяет сохранение нового товара и его последующий поиск.
   */
  @Test
  void persistsProduct() {
    // Значение для текущей операции.
    var p = service.addProduct("Груша", BigDecimal.TEN, Category.VEGETABLES, "1 кг");
    assertThat(service.search("Груша", null)).contains(p);
  }
}
