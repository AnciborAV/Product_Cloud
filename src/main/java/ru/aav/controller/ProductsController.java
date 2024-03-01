package ru.aav.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.aav.model.Product;
import ru.aav.service.ProductService;

@Controller
public class ProductsController {
    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String viewProducts(Model model) {
        var products = productService.getProducts();
        model.addAttribute("products", products);
        return "products.html";
    }

    @PostMapping("/products")
    public String addProduct(@RequestParam String name,
                             @RequestParam double price,
                             Model model) {
        var p = new Product();
        p.setName(name);
        p.setPrice(price);
        productService.saveProduct(p);

        var products = productService.getProducts();
        model.addAttribute("products", products);
        return "products.html";
    }
}
