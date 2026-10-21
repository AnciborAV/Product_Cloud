package ru.aav.cart;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.aav.account.AccountService;
import ru.aav.controller.ProductForm;
import ru.aav.order.*;

import java.security.Principal;
import java.util.*;

/**
 * POST-операции изменяют только корзину владельца сессии. GET ничего не покупает и не удаляет.
 * {@code @Controller} означает, что строки результатов выбирают HTML-шаблон или перенаправление.
 */
@Controller
public class CartController {
    /** Зависимость для операций с личной корзиной. */
    private final CartService carts;

    /** Зависимость для поиска и изменения учётных записей. */
    private final AccountService accounts;

    /** Зависимость для работы с заказами. */
    private final OrderService orders;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public CartController(CartService carts, AccountService accounts, OrderService orders) {
        this.carts = carts;
        this.accounts = accounts;
        this.orders = orders;
    }

    /**
     * Готовит корзину и контакты, выдаёт токен оформления; хранит до 20 токенов для нескольких
     * открытых вкладок. {@code @GetMapping} связывает метод с GET-запросом по указанному пути.
     */
    @GetMapping("/cart")
    public String cart(Principal principal, Model model, HttpSession session) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = accounts.view(principal.getName());
        // Случайный токен для идентификации сессии или предотвращения повторного оформления.
        String token = UUID.randomUUID().toString();
        // Несколько открытых вкладок поддерживаются; токены связаны с текущей сессией.
        synchronized (session) {
            // Ограниченный набор токенов оформления в сессии; доступ синхронизирован для нескольких
            // вкладок.
            @SuppressWarnings("unchecked")
            var tokens = (LinkedHashSet<String>) session.getAttribute("checkoutTokens");
            if (tokens == null) tokens = new LinkedHashSet<>();
            tokens.add(token);
            while (tokens.size() > 20) tokens.remove(tokens.iterator().next());
            session.setAttribute("checkoutTokens", tokens);
        }
        model.addAttribute("productForm", new ProductForm("", null));
        model.addAttribute(
                "checkout",
                new CheckoutForm(
                        account.displayName(), account.phone(), account.address(), "", token));
        model.addAttribute("cart", carts.view(principal.getName()));
        return "cart";
    }

    /**
     * Добавляет указанное количество продукта в личную корзину и переносит сообщение через
     * перенаправление. {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции
     * защищены CSRF. {@code @RequestParam} читает параметр запроса; defaultValue задаёт значение
     * при его отсутствии.
     */
    @PostMapping("/cart/add")
    public String add(
            @RequestParam int productId,
            @RequestParam(defaultValue = "1") int quantity,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            carts.add(principal.getName(), productId, quantity);
            redirect.addFlashAttribute("success", "Продукт добавлен в корзину");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/products#catalog";
    }

    /**
     * Изменяет количество в существующей позиции; недопустимое значение показывает как ошибку
     * формы. {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции защищены
     * CSRF. {@code @RequestParam} читает параметр запроса; defaultValue задаёт значение при его
     * отсутствии.
     */
    @PostMapping("/cart/update")
    public String update(
            @RequestParam int productId,
            @RequestParam int quantity,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            carts.update(principal.getName(), productId, quantity);
            redirect.addFlashAttribute("success", "Количество обновлено");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/cart";
    }

    /**
     * Удаляет позицию из корзины владельца, сохраняя продукт в его личном списке.
     * {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции защищены CSRF.
     * {@code @RequestParam} читает параметр запроса; defaultValue задаёт значение при его
     * отсутствии.
     */
    @PostMapping("/cart/remove")
    public String remove(
            @RequestParam int productId, Principal principal, RedirectAttributes redirect) {
        carts.remove(principal.getName(), productId);
        redirect.addFlashAttribute("success", "Продукт удалён из корзины");
        return "redirect:/cart";
    }

    /**
     * Очищает корзину текущего пользователя; сохранённый список продуктов остаётся.
     * {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции защищены CSRF.
     */
    @PostMapping("/cart/clear")
    public String clear(Principal principal, RedirectAttributes redirect) {
        carts.clear(principal.getName());
        redirect.addFlashAttribute("success", "Корзина очищена");
        return "redirect:/cart";
    }

    /**
     * Проверяет принадлежность токена сессии и контакты, затем оформляет заказ; при ошибке
     * восстанавливает модель страницы корзины. {@code @PostMapping} связывает метод с
     * POST-запросом; изменяющие операции защищены CSRF. {@code @Valid} запускает проверку
     * ограничений полей переданного объекта. {@code @ModelAttribute} связывает форму с моделью либо
     * добавляет общие атрибуты до вызова контроллера.
     */
    @PostMapping("/checkout")
    public String checkout(
            @Valid @ModelAttribute("checkout") CheckoutForm form,
            BindingResult errors,
            Principal principal,
            Model model,
            HttpSession session,
            HttpServletResponse response) {
        synchronized (session) {
            // Ограниченный набор токенов оформления в сессии; доступ синхронизирован для нескольких
            // вкладок.
            var tokens = session.getAttribute("checkoutTokens");
            if (!(tokens instanceof Set<?> set) || !set.contains(form.token()))
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Откройте корзину заново");
        }
        if (!errors.hasErrors()) {
            try {
                return "redirect:/orders/" + orders.checkout(principal.getName(), form);
            } catch (IllegalArgumentException ex) {
                model.addAttribute("formError", ex.getMessage());
            }
        }
        response.setStatus(400);
        model.addAttribute("productForm", new ProductForm("", null));
        model.addAttribute("cart", carts.view(principal.getName()));
        return "cart";
    }
}
