package org.example.aav.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.example.aav.dto.RegistrationForm;
import org.example.aav.dto.ReviewForm;
import org.example.aav.service.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {
  @org.springframework.beans.factory.annotation.Value("${app.mail.enabled:false}")
  private boolean mailEnabled;

  private final AccountService accounts;
  private final ProductService products;

  public AccountController(AccountService accounts, ProductService products) {
    this.accounts = accounts;
    this.products = products;
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/register")
  public String register(Model model) {
    model.addAttribute("registrationForm", new RegistrationForm());
    return "register";
  }

  @PostMapping("/register")
  public String register(
      @Valid @ModelAttribute RegistrationForm form,
      BindingResult errors,
      RedirectAttributes flash) {
    if (errors.hasErrors()) return "register";
    try {
      accounts.register(form);
    } catch (IllegalArgumentException ex) {
      errors.rejectValue("username", "registration", ex.getMessage());
      return "register";
    } catch (DataIntegrityViolationException ex) {
      errors.rejectValue("username", "duplicate", "Этот логин уже занят");
      return "register";
    }
    flash.addFlashAttribute("message", "Аккаунт создан. Войдите с вашим логином и паролем.");
    return "redirect:/login";
  }

  @GetMapping("/account")
  public String account(Principal principal, Model model) {
    populate(principal, model);
    model.addAttribute("reviewForm", new ReviewForm());
    return "account";
  }

  @GetMapping("/account/password")
  public String password(Principal principal, Model model) {
    model.addAttribute("account", accounts.user(principal.getName()));
    model.addAttribute("mailEnabled", mailEnabled);
    return "account-password";
  }

  private void populate(Principal p, Model model) {
    model.addAttribute("mailEnabled", mailEnabled);
    model.addAttribute("account", accounts.user(p.getName()));
    model.addAttribute("selectedProducts", accounts.selected(p.getName()));
    model.addAttribute("myReviews", accounts.reviews(p.getName()));
    model.addAttribute("allProducts", products.findAll());
  }

  @PostMapping("/account/contacts")
  public String contacts(Principal p, @Valid @ModelAttribute org.example.aav.dto.ContactForm form,
      BindingResult errors, RedirectAttributes flash) {
    if (errors.hasErrors()) {
      flash.addFlashAttribute("error", "Проверьте имя, фамилию, телефон и email");
    } else {
      accounts.updateContacts(p.getName(), form);
      flash.addFlashAttribute("message", "Контакты сохранены.");
    }
    return "redirect:/account";
  }

  @PostMapping("/account/order/email")
  public String emailOrder(Principal p, RedirectAttributes flash) {
    if (!mailEnabled) {
      flash.addFlashAttribute("error", "Отправка почты отключена. Администратору нужно настроить SMTP и включить MAIL_ENABLED.");
    } else {
      try {
        accounts.emailOrder(p.getName());
        flash.addFlashAttribute("message", "Заказ поставлен в очередь на отправку на " + accounts.user(p.getName()).getEmail() + ". Доставка зависит от почтового сервера.");
      } catch (IllegalArgumentException ex) {
        flash.addFlashAttribute("error", ex.getMessage());
      }
    }
    return "redirect:/account";
  }

  @PostMapping("/account/products/{id}")
  public String add(Principal p, @PathVariable UUID id, RedirectAttributes flash) {
    accounts.addSelected(p.getName(), id);
    flash.addFlashAttribute("message", "Товар сохранён в личном кабинете");
    return "redirect:/account";
  }

  @PostMapping("/account/products/{id}/remove")
  public String remove(Principal p, @PathVariable UUID id) {
    accounts.removeSelected(p.getName(), id);
    return "redirect:/account";
  }

  @PostMapping("/account/reviews")
  public String review(
      Principal p,
      @Valid @ModelAttribute ReviewForm form,
      BindingResult errors,
      Model model,
      RedirectAttributes flash) {
    if (errors.hasErrors()) {
      populate(p, model);
      return "account";
    }
    accounts.saveReview(p.getName(), form);
    flash.addFlashAttribute("message", "Отзыв опубликован во вкладке «Отзывы»");
    return "redirect:/account";
  }

  @GetMapping("/account/reviews/{id}/edit")
  public String editReview(Principal principal, @PathVariable UUID id, Model model) {
    populate(principal, model);
    model.addAttribute("reviewForm", accounts.reviewForEditing(principal.getName(), id));
    return "account";
  }

  @PostMapping("/account/reviews/{id}/remove")
  public String removeReview(Principal p, @PathVariable UUID id) {
    accounts.removeReview(p.getName(), id);
    return "redirect:/account";
  }

  @PostMapping("/account/avatar")
  public String avatar(Principal p, @RequestParam MultipartFile file, RedirectAttributes flash) {
    try {
      accounts.uploadAvatar(p.getName(), file);
      flash.addFlashAttribute("message", "Фотография обновлена");
    } catch (IllegalArgumentException ex) {
      flash.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/account";
  }

  @PostMapping("/account/avatar/remove")
  public String removeAvatar(Principal p) {
    accounts.removeAvatar(p.getName());
    return "redirect:/account";
  }

  @GetMapping("/account/avatar")
  @ResponseBody
  public ResponseEntity<byte[]> avatar(Principal p) {
    var user = accounts.user(p.getName());
    if (!user.isHasAvatar()) return ResponseEntity.notFound().build();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.IMAGE_JPEG)
        .body(user.getAvatar());
  }
}
