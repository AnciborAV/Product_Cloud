package org.example.aav.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.example.aav.service.PasswordChangeService;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Этап 7: принимаем формы смены пароля и завершаем сеансы после подтверждения. Учебная последовательность. */
@Controller
@RequestMapping("/account/password")
public class PasswordChangeController {
  // Сервис смены пароля с подтверждением по почте.
  private final PasswordChangeService passwords;
  // Список сеансов входа для их завершения после смены пароля.
  private final SessionRegistry sessions;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param passwords сервис смены пароля с подтверждением по почте
   * @param sessions список сеансов входа для их завершения после смены пароля
   */
  public PasswordChangeController(PasswordChangeService passwords, SessionRegistry sessions) {
    this.passwords = passwords;
    this.sessions = sessions;
  }

  /**
   * Принимает форму смены пароля и сообщает результат запроса кода.
   * @param principal сведения о пользователе, который вошёл в систему
   * @param currentPassword текущий пароль для проверки владельца аккаунта
   * @param newPassword новый пароль, выбранный пользователем
   * @param repeatPassword повтор нового пароля для защиты от опечатки
   * @param flash сообщения, доступные после перенаправления браузера
   */
  @PostMapping("/request")
  public String request(Principal principal, @RequestParam String currentPassword,
      @RequestParam String newPassword, @RequestParam String repeatPassword, RedirectAttributes flash) {
    try {
      passwords.request(principal.getName(), currentPassword, newPassword, repeatPassword);
      flash.addFlashAttribute("message", "Код подтверждения отправлен в очередь на ваш email. Введите его ниже в течение 15 минут.");
    // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
    } catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
    return "redirect:/account/password";
  }

  /**
   * Подтверждает смену пароля и завершает все сеансы этого пользователя.
   * @param principal сведения о пользователе, который вошёл в систему
   * @param code код подтверждения смены пароля
   * @param request HTTP-запрос браузера, необходимый для завершения текущего сеанса
   * @param flash сообщения, доступные после перенаправления браузера
   */
  @PostMapping("/confirm")
  public String confirm(Principal principal, @RequestParam String code,
      HttpServletRequest request, RedirectAttributes flash) {
    try {
      passwords.confirm(principal.getName(), code);
    // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
    } catch (IllegalArgumentException ex) {
      flash.addFlashAttribute("error", ex.getMessage());
      return "redirect:/account/password";
    }
    // Значение для текущей операции.
    for (Object p : sessions.getAllPrincipals()) {
      // user — объект после проверки и уточнения его типа через instanceof.
      if (p instanceof UserDetails user && user.getUsername().equals(principal.getName()))
        // session — сеанс входа или настройки сеансов.
        sessions.getAllSessions(p, false).forEach(session -> session.expireNow());
    }
    new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
        .logout(request, null, org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    return "redirect:/login?passwordChanged";
  }
}
