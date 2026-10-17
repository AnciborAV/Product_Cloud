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

/**
 * Веб-контроллер связывает HTTP-запросы с сервисом и HTML-шаблоном Thymeleaf.
 * @Controller означает, что возвращаемая строка — имя представления/перенаправление,
 * а не JSON-тело ответа, как было бы при @RestController.
 * Контроллер отвечает за ввод и представление; операции с БД вынесены в сервис.
 */
@Controller
public class ProductsController {
    private final ProductService productService;

    /**
     * Spring передаёт готовый сервис через конструктор; контроллер не создаёт его сам.
     */
    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Перенаправляет посетителя с главной страницы на каталог.
     * Префикс redirect: заставляет Spring отправить браузеру перенаправление.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }

    /**
     * Обрабатывает GET /products?page=0 и готовит страницу каталога.
     * @RequestParam читает номер из URL; если параметр отсутствует, берётся 0.
     * Model переносит именованные атрибуты в Thymeleaf. productForm нужен для
     * привязки полей формы, products — для таблицы и навигации по страницам.
     * Строка products разрешается в resources/templates/products.html.
     */
    @GetMapping("/products")
    public String viewProducts(@RequestParam(defaultValue = "0") int page, Model model) {
        // Явно отклоняем отрицательные и чрезмерно большие номера ответом HTTP 400.
        if (page < 0 || page > 1_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        }
        model.addAttribute("productForm", new ProductForm("", null));
        model.addAttribute("products", productService.getProducts(page));
        return "products";
    }

    /**
     * Обрабатывает отправку формы POST /products.
     * @ModelAttribute создаёт ProductForm из параметров, @Valid запускает проверки.
     * BindingResult должен идти сразу после проверяемого аргумента: здесь Spring
     * передаёт ошибки преобразования и валидации, чтобы показать их рядом с полями.
     * При ошибке возвращается HTTP 400 и та же форма с введёнными значениями.
     * При успехе Post/Redirect/Get переводит браузер на GET: обновление страницы
     * не повторяет предыдущий POST (но это не защита от двойного клика/повтора запроса).
     * Flash-атрибут переносит сообщение об успехе через ближайшее перенаправление.
     */
    @PostMapping("/products")
    public String addProduct(@Valid @ModelAttribute ProductForm productForm,
                             BindingResult errors, Model model,
                             RedirectAttributes redirect, HttpServletResponse response) {
        if (errors.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            // Повторно заполняем каталог: новая HTML-страница нуждается в этих данных.
            model.addAttribute("products", productService.getProducts(0));
            return "products";
        }
        productService.createProduct(productForm.name(), productForm.price());
        redirect.addFlashAttribute("success", "Товар добавлен");
        return "redirect:/products";
    }
}
