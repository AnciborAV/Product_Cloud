package ru.aav.order;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

/** Чужой номер заказа не раскрывает состав или контакты: сервис всегда проверяет владельца. */
@Controller
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders) { this.orders = orders; }
    @GetMapping("/orders")
    public String history(@RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        model.addAttribute("orders", orders.history(principal.getName(), page)); return "orders";
    }
    @GetMapping("/orders/{id}")
    public String order(@PathVariable long id, Principal principal, Model model) {
        model.addAttribute("order", orders.get(principal.getName(), id)); return "order";
    }
    @PostMapping("/orders/{id}/repeat")
    public String repeat(@PathVariable long id, Principal principal, RedirectAttributes redirect) {
        try {
            orders.repeat(principal.getName(), id);
            redirect.addFlashAttribute("success", "Состав заказа добавлен в корзину по актуальным ценам");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("formError", ex.getMessage()); }
        return "redirect:/cart";
    }
}
