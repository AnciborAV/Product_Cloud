package org.example.aav.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.example.aav.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductServiceTest {
  @Autowired ProductService service;

  @Test
  void searchesIgnoringCaseAndWhitespace() {
    assertThat(service.search("  МОЛОКО ", null))
        .singleElement()
        .satisfies(p -> assertThat(p.name()).contains("Молоко"));
  }

  @Test
  void combinesSearchAndCategory() {
    assertThat(service.search("молоко", Category.BAKERY)).isEmpty();
    assertThat(service.search(null, Category.DAIRY)).hasSize(5);
  }

  @Test
  void treatsSqlWildcardsLiterally() {
    assertThat(service.search("%", null)).allSatisfy(p -> assertThat(p.name()).contains("%"));
    assertThat(service.search("_", null)).isEmpty();
  }

  @Test
  void seededCatalogHasTenDepartmentsAndFiftyLocalPhotos() throws Exception {
    var catalog = service.findAll().stream().filter(p -> p.getImageUrl() != null).toList();
    assertThat(catalog).hasSize(50);
    assertThat(catalog.stream().map(p -> p.category()).distinct()).hasSize(10);
    for (var product : catalog) {
      var resource = new org.springframework.core.io.ClassPathResource("static" + product.getImageUrl());
      assertThat(resource.exists()).as(product.name()).isTrue();
      try (var input = resource.getInputStream()) {
        assertThat(javax.imageio.ImageIO.read(input)).as(product.name()).isNotNull();
      }
    }
  }

  @Test
  void persistsProduct() {
    var p = service.addProduct("Груша", BigDecimal.TEN, Category.VEGETABLES, "1 кг");
    assertThat(service.search("Груша", null)).contains(p);
  }
}
