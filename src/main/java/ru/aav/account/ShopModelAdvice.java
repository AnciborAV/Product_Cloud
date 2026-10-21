package ru.aav.account;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import ru.aav.cart.CartService;

import java.security.Principal;

/**
 * Общая шапка получает только безопасный профиль и количество продуктов текущего пользователя.
 * {@code @ControllerAdvice} применяет общую подготовку модели или обработку исключений к
 * контроллерам.
 */
@ControllerAdvice(annotations = Controller.class)
public class ShopModelAdvice {
    /** Зависимость для поиска и изменения учётных записей. */
    private final AccountService accounts;

    /** Зависимость для операций с личной корзиной. */
    private final CartService carts;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public ShopModelAdvice(AccountService accounts, CartService carts) {
        this.accounts = accounts;
        this.carts = carts;
    }

    /**
     * Перед рендерингом добавляет безопасный профиль и количество продуктов в общую шапку; для
     * гостя задаёт пустые значения. {@code @ModelAttribute} связывает форму с моделью либо
     * добавляет общие атрибуты до вызова контроллера.
     */
    @ModelAttribute
    public void shopContext(Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("viewer", accounts.view(principal.getName()));
            model.addAttribute("cartQuantity", carts.quantity(principal.getName()));
        } else {
            model.addAttribute("viewer", null);
            model.addAttribute("cartQuantity", 0);
        }
    }
}
