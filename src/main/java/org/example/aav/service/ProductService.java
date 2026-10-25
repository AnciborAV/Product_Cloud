package org.example.aav.service;

import java.math.BigDecimal;
import java.util.*;
import org.example.aav.model.*;
import org.example.aav.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Этап 5: собираем правила добавления и поиска товаров отдельно от обработки запросов. Учебная последовательность. */
@Service
@Transactional(readOnly = true)
public class ProductService {
  // Доступ к таблице товаров в базе.
  private final ProductRepository products;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param products репозиторий для чтения и сохранения товаров в базе
   */
  public ProductService(ProductRepository products) {
    this.products = products;
  }

  /**
   * Проверяет данные товара через его модель и сохраняет товар в каталоге.
   * @param name название товара
   * @param price цена товара; BigDecimal позволяет точно хранить копейки
   * @param category категория товара
   * @param unit единица продажи, например 1 кг или 1 шт.
   */
  @Transactional
  public Product addProduct(String name, BigDecimal price, Category category, String unit) {
    return products
        .save(new ProductEntity(new Product(UUID.randomUUID(), name, price, category, unit)))
        .toProduct();
  }

  /**
   * Возвращает все товары, отсортированные по названию.
   */
  public List<Product> findAll() {
    return products.findAll(Sort.by("name")).stream().map(ProductEntity::toProduct).toList();
  }

  /**
   * Ищет товары по тексту и категории; возвращает содержимое первой страницы.
   * @param query текст поиска товара
   * @param category категория товара
   */
  public List<Product> search(String query, Category category) {
    return searchPage(query, category, 0).getContent();
  }

  /**
   * Подготавливает строку поиска и загружает страницу из 12 товаров.
   * @param query текст поиска товара
   * @param category категория товара
   * @param page номер страницы, начиная с нуля
   */
  public Page<Product> searchPage(String query, Category category, int page) {
    // Строка поиска товара.
    String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    q = q.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    return products
        .search(q, category, PageRequest.of(Math.max(0, page), 12, Sort.by("name")))
        .map(ProductEntity::toProduct);
  }
}
