package ru.aav.cart;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.*;
import ru.aav.account.AccountService;
import ru.aav.order.*;

/** POST-операции изменяют только корзину владельца сессии. GET ничего не покупает и не удаляет. */
@Controller
public class CartController {
    private final CartService carts;
    private final AccountService accounts;
    private final OrderService orders;
    public CartController(CartService carts, AccountService accounts, OrderService orders) {
        this.carts = carts; this.accounts = accounts; this.orders = orders;
    }
    @GetMapping("/cart")
    public String cart(Principal principal, Model model, HttpSession session) {
        var a = accounts.view(principal.getName());
        String token = UUID.randomUUID().toString();
        // Несколько открытых вкладок поддерживаются; токены связаны с текущей сессией.
        synchronized (session) {
            @SuppressWarnings("unchecked")
            var tokens = (LinkedHashSet<String>) session.getAttribute("checkoutTokens");
            if (tokens == null) tokens = new LinkedHashSet<>();
            tokens.add(token);
            while (tokens.size() > 20) tokens.remove(tokens.iterator().next());
            session.setAttribute("checkoutTokens", tokens);
        }
        model.addAttribute("productForm", new ru.aav.controller.ProductForm("", null));
        model.addAttribute("checkout", new CheckoutForm(a.displayName(), a.phone(), a.address(), "", token));
        model.addAttribute("cart", carts.view(principal.getName()));
        return "cart";
    }
    @PostMapping("/cart/add")
    public String add(@RequestParam int productId, @RequestParam(defaultValue = "1") int quantity,
                      Principal principal, RedirectAttributes redirect) {
        try { carts.add(principal.getName(), productId, quantity); redirect.addFlashAttribute("success", "Продукт добавлен в корзину"); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("formError", ex.getMessage()); }
        return "redirect:/products#catalog";
    }
    @PostMapping("/cart/update")
    public String update(@RequestParam int productId, @RequestParam int quantity, Principal principal, RedirectAttributes redirect) {
        try { carts.update(principal.getName(), productId, quantity); redirect.addFlashAttribute("success", "Количество обновлено"); }
        catch (IllegalArgumentException ex) { redirect.addFlashAttribute("formError", ex.getMessage()); }
        return "redirect:/cart";
    }
    @PostMapping("/cart/remove")
    public String remove(@RequestParam int productId, Principal principal, RedirectAttributes redirect) {
        carts.remove(principal.getName(), productId); redirect.addFlashAttribute("success", "Продукт удалён из корзины");
        return "redirect:/cart";
    }
    @PostMapping("/cart/clear")
    public String clear(Principal principal, RedirectAttributes redirect) {
        carts.clear(principal.getName()); redirect.addFlashAttribute("success", "Корзина очищена"); return "redirect:/cart";
    }
    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute("checkout") CheckoutForm form, BindingResult errors,
                           Principal principal, Model model, HttpSession session, HttpServletResponse response) {
        synchronized (session) {
            var tokens = session.getAttribute("checkoutTokens");
            if (!(tokens instanceof Set<?> set) || !set.contains(form.token()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Откройте корзину заново");
        }
        if (!errors.hasErrors()) {
            try { return "redirect:/orders/" + orders.checkout(principal.getName(), form); }
            catch (IllegalArgumentException ex) { model.addAttribute("formError", ex.getMessage()); }
        }
        response.setStatus(400);
        model.addAttribute("productForm", new ru.aav.controller.ProductForm("", null));
        model.addAttribute("cart", carts.view(principal.getName()));
        return "cart";
    }
}
