package org.example.aav.controller;

import jakarta.validation.Valid;
import org.example.aav.dto.ProductForm;
import org.example.aav.model.Category;
import org.example.aav.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Этап 7: связываем запросы витрины с сервисом товаров и HTML-шаблоном. Учебная последовательность. */
@Controller
public class ProductsController {
  // Сервис добавления и поиска товаров.
  private final ProductService productService;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param productService сервис добавления и поиска товаров
   */
  public ProductsController(ProductService productService) {
    this.productService = productService;
  }

  /**
   * Проверяет наличие роли администратора, чтобы показать доступные ему действия.
   * @param auth сведения о входе и правах пользователя
   */
  @ModelAttribute("isAdmin")
  public boolean isAdmin(org.springframework.security.core.Authentication auth) {
    return auth != null
        // a — очередное право доступа пользователя.
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  /**
   * Передаёт шаблону все категории для списка выбора.
   */
  @ModelAttribute("categories")
  public Category[] categories() {
    return Category.values();
  }

  /**
   * Показывает витрину с поиском, фильтром категории и страницами товаров.
   * @param q строка поиска товара
   * @param category категория товара
   * @param page номер страницы, начиная с нуля
   * @param model данные, которые Spring передаст HTML-шаблону
   */
  @GetMapping({"/", "/products"})
  public String viewProducts(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Category category,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    model.addAttribute("productForm", new ProductForm());
    populateCatalog(q, category, model, page);
    return "products";
  }

  /**
   * Проверяет форму товара; при успехе сохраняет товар и перенаправляет на витрину.
   * @param form данные формы, которые нужно проверить или заполнить
   * @param errors ошибки заполнения и проверки формы
   * @param model данные, которые Spring передаст HTML-шаблону
   * @param redirect сообщения для следующей страницы после перенаправления
   */
  @PostMapping("/products")
  public String addProduct(
      @Valid @ModelAttribute("productForm") ProductForm form,
      BindingResult errors,
      Model model,
      RedirectAttributes redirect) {
    if (errors.hasErrors()) {
      populateCatalog("", null, model, 0);
      return "products";
    }
    productService.addProduct(form.getName(), form.getPrice(), form.getCategory(), form.getUnit());
    redirect.addFlashAttribute("message", "Товар добавлен в витрину");
    return "redirect:/products";
  }

  /**
   * Добавляет найденные товары и параметры поиска в данные HTML-шаблона.
   * @param query текст поиска товара
   * @param category категория товара
   * @param model данные, которые Spring передаст HTML-шаблону
   * @param page номер страницы, начиная с нуля
   */
  private void populateCatalog(String query, Category category, Model model, int page) {
    // Страница найденных товаров вместе с общим числом страниц.
    var result = productService.searchPage(query, category, page);
    model.addAttribute("products", result.getContent());
    model.addAttribute("productPage", result);
    model.addAttribute("q", query);
    model.addAttribute("selectedCategory", category);
  }
}
