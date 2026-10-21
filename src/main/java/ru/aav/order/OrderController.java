package ru.aav.order;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Чужой номер заказа не раскрывает состав или контакты: сервис всегда проверяет владельца.
 * {@code @Controller} означает, что строки результатов выбирают HTML-шаблон или перенаправление.
 */
@Controller
public class OrderController {
    /** Зависимость для работы с заказами. */
    private final OrderService orders;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /**
     * Передаёт страницу истории владельца в шаблон orders.html. {@code @GetMapping} связывает метод
     * с GET-запросом по указанному пути. {@code @RequestParam} читает параметр запроса;
     * defaultValue задаёт значение при его отсутствии.
     */
    @GetMapping("/orders")
    public String history(
            @RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        model.addAttribute("orders", orders.history(principal.getName(), page));
        return "orders";
    }

    /**
     * Показывает состав и контакты только собственного заказа. {@code @GetMapping} связывает метод
     * с GET-запросом по указанному пути. {@code @PathVariable} извлекает идентификатор из пути URL.
     */
    @GetMapping("/orders/{id}")
    public String order(@PathVariable long id, Principal principal, Model model) {
        model.addAttribute("order", orders.get(principal.getName(), id));
        return "order";
    }

    /**
     * Повторяет состав заказа в корзине и показывает сообщение об успехе либо причину отказа.
     * {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции защищены CSRF.
     * {@code @PathVariable} извлекает идентификатор из пути URL.
     */
    @PostMapping("/orders/{id}/repeat")
    public String repeat(@PathVariable long id, Principal principal, RedirectAttributes redirect) {
        try {
            orders.repeat(principal.getName(), id);
            redirect.addFlashAttribute(
                    "success", "Состав заказа добавлен в корзину по актуальным ценам");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/cart";
    }
}
