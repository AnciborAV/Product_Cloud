package ru.aav.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import ru.aav.order.OrderService;

import java.security.Principal;

/**
 * Страницы регистрации, входа и профиля. Каждый личный запрос получает владельца из сессии.
 * {@code @Controller} означает, что строки результатов выбирают HTML-шаблон или перенаправление.
 */
@Controller
public class AccountController {
    /** Зависимость для поиска и изменения учётных записей. */
    private final AccountService accounts;

    /** Сервис проверки и преобразования загруженных фото. */
    private final AvatarService avatars;

    /** Зависимость для работы с заказами. */
    private final OrderService orders;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public AccountController(AccountService accounts, AvatarService avatars, OrderService orders) {
        this.accounts = accounts;
        this.avatars = avatars;
        this.orders = orders;
    }

    /**
     * Показывает форму входа гостю или перенаправляет вошедшего пользователя в кабинет.
     * {@code @GetMapping} связывает метод с GET-запросом по указанному пути.
     */
    @GetMapping("/login")
    public String login(Principal principal) {
        return principal == null ? "login" : "redirect:/account";
    }

    /**
     * Показывает форму регистрации либо обрабатывает её отправку: проверяет данные и фото, создаёт
     * аккаунт и возвращает ошибки без вывода пароля. {@code @GetMapping} связывает метод с
     * GET-запросом по указанному пути.
     */
    @GetMapping("/register")
    public String register(Principal principal, Model model) {
        if (principal != null) return "redirect:/account";
        model.addAttribute("registration", new AccountForms.Registration("", "", "", ""));
        return "register";
    }

    /**
     * Показывает форму регистрации либо обрабатывает её отправку: проверяет данные и фото, создаёт
     * аккаунт и возвращает ошибки без вывода пароля. {@code @PostMapping} связывает метод с
     * POST-запросом; изменяющие операции защищены CSRF. {@code @Valid} запускает проверку
     * ограничений полей переданного объекта. {@code @ModelAttribute} связывает форму с моделью либо
     * добавляет общие атрибуты до вызова контроллера. {@code @RequestParam} читает параметр
     * запроса; defaultValue задаёт значение при его отсутствии.
     */
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registration") AccountForms.Registration form,
            BindingResult errors,
            @RequestParam(required = false) MultipartFile avatar,
            Model model,
            RedirectAttributes redirect,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!errors.hasErrors()) {
            try {
                accounts.register(form, avatars.normalize(avatar));
                redirect.addFlashAttribute(
                        "success", "Кабинет создан. Войдите с вашим email и паролем");
                return "redirect:/login";
            } catch (IllegalArgumentException ex) {
                model.addAttribute("formError", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                model.addAttribute(
                        "formError", "Этот email уже зарегистрирован. Войдите в кабинет");
            }
        }
        response.setStatus(400);
        return "register";
    }

    /**
     * Готовит данные кабинета и выбирает шаблон account.html. {@code @GetMapping} связывает метод с
     * GET-запросом по указанному пути.
     */
    @GetMapping("/account")
    public String account(Principal principal, Model model) {
        populate(principal, model);
        return "account";
    }

    /**
     * Добавляет профиль, пустую форму пароля и последние заказы; сохраняет введённые пользователем
     * данные при ошибке формы.
     */
    private void populate(Principal principal, Model model) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = accounts.view(principal.getName());
        if (!model.containsAttribute("profile"))
            model.addAttribute(
                    "profile",
                    new AccountForms.Profile(
                            account.displayName(), account.phone(), account.address()));
        if (!model.containsAttribute("passwordForm"))
            model.addAttribute("passwordForm", new AccountForms.Password("", "", ""));
        model.addAttribute("recentOrders", orders.history(principal.getName(), 0));
    }

    /**
     * Сохраняет проверенные контактные данные владельца сессии; ошибки возвращает в той же форме.
     * {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции защищены CSRF.
     * {@code @Valid} запускает проверку ограничений полей переданного объекта.
     * {@code @ModelAttribute} связывает форму с моделью либо добавляет общие атрибуты до вызова
     * контроллера.
     */
    @PostMapping("/account/profile")
    public String profile(
            @Valid @ModelAttribute("profile") AccountForms.Profile form,
            BindingResult errors,
            Principal principal,
            Model model,
            RedirectAttributes redirect,
            HttpServletResponse response) {
        if (errors.hasErrors()) {
            response.setStatus(400);
            populate(principal, model);
            return "account";
        }
        accounts.update(principal.getName(), form);
        redirect.addFlashAttribute("success", "Профиль сохранён");
        return "redirect:/account";
    }

    /**
     * Для POST проверяет и сохраняет фото владельца; для GET возвращает его JPEG без кеширования
     * или HTTP 404, если фото отсутствует. {@code @PostMapping} связывает метод с POST-запросом;
     * изменяющие операции защищены CSRF. {@code @RequestParam} читает параметр запроса;
     * defaultValue задаёт значение при его отсутствии.
     */
    @PostMapping("/account/avatar")
    public String avatar(
            @RequestParam MultipartFile avatar, Principal principal, RedirectAttributes redirect) {
        try {
            // Буфер байтов текущей операции; после подготовки преобразуется в ответ или сохраняемое
            // значение.
            var bytes = avatars.normalize(avatar);
            if (bytes == null) throw new IllegalArgumentException("Сначала выберите фотографию");
            accounts.avatar(principal.getName(), bytes);
            redirect.addFlashAttribute("success", "Фотография обновлена");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("formError", ex.getMessage());
        }
        return "redirect:/account#photo";
    }

    /**
     * Удаляет только аватар текущего пользователя и возвращает его к разделу фотографии.
     * {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции защищены CSRF.
     */
    @PostMapping("/account/avatar/remove")
    public String removeAvatar(Principal principal, RedirectAttributes redirect) {
        accounts.avatar(principal.getName(), null);
        redirect.addFlashAttribute("success", "Фотография удалена");
        return "redirect:/account#photo";
    }

    /**
     * Для POST проверяет и сохраняет фото владельца; для GET возвращает его JPEG без кеширования
     * или HTTP 404, если фото отсутствует. {@code @GetMapping} связывает метод с GET-запросом по
     * указанному пути. {@code @ResponseBody} записывает результат в тело HTTP-ответа, а не выбирает
     * шаблон.
     */
    @GetMapping("/account/avatar")
    @ResponseBody
    public ResponseEntity<byte[]> avatar(Principal principal) {
        // Копия JPEG текущего пользователя; null означает отсутствие аватара.
        byte[] image = accounts.avatar(principal.getName());
        if (image == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }

    /**
     * Проверяет текущий пароль, сохраняет новый хеш и завершает текущую сессию; при ошибке повторно
     * показывает форму. {@code @PostMapping} связывает метод с POST-запросом; изменяющие операции
     * защищены CSRF. {@code @Valid} запускает проверку ограничений полей переданного объекта.
     * {@code @ModelAttribute} связывает форму с моделью либо добавляет общие атрибуты до вызова
     * контроллера.
     */
    @PostMapping("/account/password")
    public String password(
            @Valid @ModelAttribute("passwordForm") AccountForms.Password form,
            BindingResult errors,
            Principal principal,
            Authentication authentication,
            Model model,
            RedirectAttributes redirect,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!errors.hasErrors()) {
            try {
                accounts.password(principal.getName(), form);
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                return "redirect:/login?passwordChanged";
            } catch (IllegalArgumentException ex) {
                model.addAttribute("formError", ex.getMessage());
            }
        }
        response.setStatus(400);
        populate(principal, model);
        return "account";
    }
}
