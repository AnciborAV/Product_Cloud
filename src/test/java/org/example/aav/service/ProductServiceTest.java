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
    assertThat(service.search(null, Category.DAIRY)).hasSize(2);
  }

  @Test
  void treatsSqlWildcardsLiterally() {
    assertThat(service.search("%", null)).hasSize(1);
    assertThat(service.search("_", null)).isEmpty();
  }

  @Test
  void persistsProduct() {
    var p = service.addProduct("Груша", BigDecimal.TEN, Category.VEGETABLES, "1 кг");
    assertThat(service.search("Груша", null)).contains(p);
  }
}
