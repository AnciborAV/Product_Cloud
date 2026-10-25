package org.example.aav.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.example.aav.service.PasswordChangeService;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/password")
public class PasswordChangeController {
  private final PasswordChangeService passwords;
  private final SessionRegistry sessions;

  public PasswordChangeController(PasswordChangeService passwords, SessionRegistry sessions) {
    this.passwords = passwords;
    this.sessions = sessions;
  }

  @PostMapping("/request")
  public String request(Principal principal, @RequestParam String currentPassword,
      @RequestParam String newPassword, @RequestParam String repeatPassword, RedirectAttributes flash) {
    try {
      passwords.request(principal.getName(), currentPassword, newPassword, repeatPassword);
      flash.addFlashAttribute("message", "Код подтверждения отправлен в очередь на ваш email. Введите его ниже в течение 15 минут.");
    } catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
    return "redirect:/account#password";
  }

  @PostMapping("/confirm")
  public String confirm(Principal principal, @RequestParam String code,
      HttpServletRequest request, RedirectAttributes flash) {
    try {
      passwords.confirm(principal.getName(), code);
    } catch (IllegalArgumentException ex) {
      flash.addFlashAttribute("error", ex.getMessage());
      return "redirect:/account#password";
    }
    for (Object p : sessions.getAllPrincipals()) {
      if (p instanceof UserDetails user && user.getUsername().equals(principal.getName()))
        sessions.getAllSessions(p, false).forEach(session -> session.expireNow());
    }
    new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
        .logout(request, null, org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    return "redirect:/login?passwordChanged";
  }
}
