package org.example.aav.service;

import org.example.aav.model.Category;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

class ProductServiceTest {
    private final ProductService service = new ProductService();

    @Test
    void searchesIgnoringCaseAndWhitespace() {
        assertThat(service.search("  МОЛОКО ", null)).singleElement().satisfies(product -> assertThat(product.name()).contains("Молоко"));
    }

    @Test
    void combinesSearchAndCategory() {
        assertThat(service.search("молоко", Category.BAKERY)).isEmpty();
        assertThat(service.search(null, Category.DAIRY)).hasSize(2).allSatisfy(product -> assertThat(product.category()).isEqualTo(Category.DAIRY));
    }

    @Test
    void returnsImmutableSnapshot() {
        var snapshot = service.findAll();
        service.addProduct("Груша", BigDecimal.TEN, Category.VEGETABLES, "1 кг");
        assertThat(snapshot).hasSize(8);
        assertThat(service.findAll()).hasSize(9);
        assertThatThrownBy(snapshot::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void handlesConcurrentAddsWithoutLosingProducts() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = IntStream.range(0, 100).<Callable<Void>>mapToObj(index -> () -> {
                service.addProduct("Товар " + index, BigDecimal.ONE, Category.PANTRY, "1 шт.");
                return null;
            }).toList();
            for (var result : executor.invokeAll(tasks)) { result.get(); }
        }
        assertThat(service.findAll()).hasSize(108);
        assertThat(service.findAll()).extracting(product -> product.id()).doesNotHaveDuplicates();
    }
}
