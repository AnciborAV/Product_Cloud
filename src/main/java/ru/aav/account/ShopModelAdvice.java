package ru.aav.account;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import ru.aav.cart.CartService;

/** Общая шапка получает только безопасный профиль и количество продуктов текущего пользователя. */
@ControllerAdvice(annotations = Controller.class)
public class ShopModelAdvice {
    private final AccountService accounts;
    private final CartService carts;
    public ShopModelAdvice(AccountService accounts, CartService carts) { this.accounts = accounts; this.carts = carts; }
    @ModelAttribute
    public void shopContext(Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("viewer", accounts.view(principal.getName()));
            model.addAttribute("cartQuantity", carts.view(principal.getName()).quantity());
        } else {
            model.addAttribute("viewer", null);
            model.addAttribute("cartQuantity", 0);
        }
    }
}
