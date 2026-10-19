package ru.aav.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import ru.aav.order.OrderService;

/** Страницы регистрации, входа и профиля. Каждый личный запрос получает владельца из сессии. */
@Controller
public class AccountController {
    private final AccountService accounts;
    private final AvatarService avatars;
    private final OrderService orders;
    public AccountController(AccountService accounts, AvatarService avatars, OrderService orders) {
        this.accounts = accounts; this.avatars = avatars; this.orders = orders;
    }
    @GetMapping("/login")
    public String login(Principal principal) { return principal == null ? "login" : "redirect:/account"; }
    @GetMapping("/register")
    public String register(Principal principal, Model model) {
        if (principal != null) return "redirect:/account";
        model.addAttribute("registration", new AccountForms.Registration("", "", "", ""));
        return "register";
    }
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") AccountForms.Registration form, BindingResult errors,
                           @RequestParam(required = false) MultipartFile avatar, Model model, RedirectAttributes redirect,
                           HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        if (!errors.hasErrors()) {
            try {
                accounts.register(form, avatars.normalize(avatar));
                redirect.addFlashAttribute("success", "Кабинет создан. Войдите с вашим email и паролем");
                return "redirect:/login";
            } catch (IllegalArgumentException ex) { model.addAttribute("formError", ex.getMessage()); }
            catch (DataIntegrityViolationException ex) { model.addAttribute("formError", "Этот email уже зарегистрирован. Войдите в кабинет"); }
        }
        response.setStatus(400);
        return "register";
    }
    @GetMapping("/account")
    public String account(Principal principal, Model model) {
        populate(principal, model);
        return "account";
    }
    private void populate(Principal principal, Model model) {
        var a = accounts.view(principal.getName());
        if (!model.containsAttribute("profile")) model.addAttribute("profile", new AccountForms.Profile(a.displayName(), a.phone(), a.address()));
        if (!model.containsAttribute("passwordForm")) model.addAttribute("passwordForm", new AccountForms.Password("", "", ""));
        model.addAttribute("recentOrders", orders.history(principal.getName(), 0));
    }
    @PostMapping("/account/profile")
    public String profile(@Valid @ModelAttribute("profile") AccountForms.Profile form, BindingResult errors,
                          Principal principal, Model model, RedirectAttributes redirect, jakarta.servlet.http.HttpServletResponse response) {
        if (errors.hasErrors()) { response.setStatus(400); populate(principal, model); return "account"; }
        accounts.update(principal.getName(), form);
        redirect.addFlashAttribute("success", "Профиль сохранён");
        return "redirect:/account";
    }
    @PostMapping("/account/avatar")
    public String avatar(@RequestParam MultipartFile avatar, Principal principal, RedirectAttributes redirect) {
        try {
            var bytes = avatars.normalize(avatar);
            if (bytes == null) throw new IllegalArgumentException("Сначала выберите фотографию");
            accounts.avatar(principal.getName(), bytes);
            redirect.addFlashAttribute("success", "Фотография обновлена");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("formError", ex.getMessage()); }
        return "redirect:/account#photo";
    }
    @PostMapping("/account/avatar/remove")
    public String removeAvatar(Principal principal, RedirectAttributes redirect) {
        accounts.avatar(principal.getName(), null);
        redirect.addFlashAttribute("success", "Фотография удалена");
        return "redirect:/account#photo";
    }
    @GetMapping("/account/avatar")
    @ResponseBody
    public ResponseEntity<byte[]> avatar(Principal principal) {
        byte[] image = accounts.avatar(principal.getName());
        if (image == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG).body(image);
    }
    @PostMapping("/account/password")
    public String password(@Valid @ModelAttribute("passwordForm") AccountForms.Password form, BindingResult errors,
                           Principal principal, Authentication authentication, Model model, RedirectAttributes redirect,
                           HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        if (!errors.hasErrors()) {
            try {
                accounts.password(principal.getName(), form);
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                return "redirect:/login?passwordChanged";
            } catch (IllegalArgumentException ex) { model.addAttribute("formError", ex.getMessage()); }
        }
        response.setStatus(400); populate(principal, model); return "account";
    }
}
