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

@Controller
public class ProductsController {
  private final ProductService productService;

  public ProductsController(ProductService productService) {
    this.productService = productService;
  }

  @ModelAttribute("isAdmin")
  public boolean isAdmin(org.springframework.security.core.Authentication auth) {
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  @ModelAttribute("categories")
  public Category[] categories() {
    return Category.values();
  }

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

  private void populateCatalog(String query, Category category, Model model, int page) {
    var result = productService.searchPage(query, category, page);
    model.addAttribute("products", result.getContent());
    model.addAttribute("productPage", result);
    model.addAttribute("q", query);
    model.addAttribute("selectedCategory", category);
  }
}
