package ru.aav.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.aav.service.ProductService;

@Controller
public class ProductsController {
    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String viewProducts(@RequestParam(defaultValue = "0") int page, Model model) {
        if (page < 0 || page > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        }
        model.addAttribute("productForm", new ProductForm("", null));
        model.addAttribute("products", productService.getProducts(page));
        return "products";
    }

    @PostMapping("/products")
    public String addProduct(@Valid @ModelAttribute ProductForm productForm,
                             BindingResult errors, Model model,
                             RedirectAttributes redirect, HttpServletResponse response) {
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            model.addAttribute("products", productService.getProducts(0));
            return "products";
        }
        productService.createProduct(productForm.name(), productForm.price());
        redirect.addFlashAttribute("success", "Товар добавлен");
        return "redirect:/products";
    }
}
