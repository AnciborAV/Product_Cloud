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

/** Этап 7: связываем страницы регистрации и личного кабинета с сервисами приложения. Учебная последовательность. */
@Controller
public class AccountController {
  // Включена ли отправка почты в настройках.
  @org.springframework.beans.factory.annotation.Value("${app.mail.enabled:false}")
  private boolean mailEnabled;

  // Сервис регистрации и действий в личном кабинете.
  private final AccountService accounts;
  // Сервис поиска и добавления товаров.
  private final ProductService products;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param accounts сервис регистрации и действий в личном кабинете
   * @param products сервис получения товаров для личного кабинета
   */
  public AccountController(AccountService accounts, ProductService products) {
    this.accounts = accounts;
    this.products = products;
  }

  /**
   * Показывает страницу входа; сам пароль проверяет Spring Security.
   */
  @GetMapping("/login")
  public String login() {
    return "login";
  }

  /**
   * Показывает форму регистрации или проверяет и сохраняет нового пользователя.
   * @param model данные, которые Spring передаст HTML-шаблону
   */
  @GetMapping("/register")
  public String register(Model model) {
    model.addAttribute("registrationForm", new RegistrationForm());
    return "register";
  }

  /**
   * Показывает форму регистрации или проверяет и сохраняет нового пользователя.
   * @param form данные формы, которые нужно проверить или заполнить
   * @param errors ошибки заполнения и проверки формы
   * @param flash сообщения, доступные после перенаправления браузера
   */
  @PostMapping("/register")
  public String register(
      @Valid @ModelAttribute RegistrationForm form,
      BindingResult errors,
      RedirectAttributes flash) {
    if (errors.hasErrors()) return "register";
    try {
      accounts.register(form);
    // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
    } catch (IllegalArgumentException ex) {
      errors.rejectValue("username", "registration", ex.getMessage());
      return "register";
    // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
    } catch (DataIntegrityViolationException ex) {
      errors.rejectValue("username", "duplicate", "Этот логин уже занят");
      return "register";
    }
    flash.addFlashAttribute("message", "Аккаунт создан. Войдите с вашим логином и паролем.");
    return "redirect:/login";
  }

  /**
   * Заполняет данными страницу личного кабинета.
   * @param principal сведения о пользователе, который вошёл в систему
   * @param model данные, которые Spring передаст HTML-шаблону
   */
  @GetMapping("/account")
  public String account(Principal principal, Model model) {
    populate(principal, model);
    model.addAttribute("reviewForm", new ReviewForm());
    return "account";
  }

  /**
   * Показывает страницу настроек смены пароля.
   * @param principal сведения о пользователе, который вошёл в систему
   * @param model данные, которые Spring передаст HTML-шаблону
   */
  @GetMapping("/account/password")
  public String password(Principal principal, Model model) {
    model.addAttribute("account", accounts.user(principal.getName()));
    model.addAttribute("mailEnabled", mailEnabled);
    return "account-password";
  }

  /**
   * Собирает данные пользователя, товаров и отзывов для шаблона личного кабинета.
   * @param p вошедший пользователь
   * @param model данные, которые Spring передаст HTML-шаблону
   */
  private void populate(Principal p, Model model) {
    model.addAttribute("mailEnabled", mailEnabled);
    model.addAttribute("account", accounts.user(p.getName()));
    model.addAttribute("selectedProducts", accounts.selected(p.getName()));
    model.addAttribute("myReviews", accounts.reviews(p.getName()));
    model.addAttribute("allProducts", products.findAll());
  }

  /**
   * Проверяет форму контактов и сообщает пользователю результат сохранения.
   * @param p вошедший пользователь
   * @param form данные формы, которые нужно проверить или заполнить
   * @param errors ошибки заполнения и проверки формы
   * @param flash сообщения, доступные после перенаправления браузера
   */
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

  /**
   * Ставит письмо с выбранными товарами в очередь; сообщает об отсутствии email или товаров.
   * @param p вошедший пользователь
   * @param flash сообщения, доступные после перенаправления браузера
   */
  @PostMapping("/account/order/email")
  public String emailOrder(Principal p, RedirectAttributes flash) {
    if (!mailEnabled) {
      flash.addFlashAttribute("error", "Отправка почты отключена. Администратору нужно настроить SMTP и включить MAIL_ENABLED.");
    } else {
      try {
        accounts.emailOrder(p.getName());
        flash.addFlashAttribute("message", "Заказ поставлен в очередь на отправку на " + accounts.user(p.getName()).getEmail() + ". Доставка зависит от почтового сервера.");
      // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
      } catch (IllegalArgumentException ex) {
        flash.addFlashAttribute("error", ex.getMessage());
      }
    }
    return "redirect:/account";
  }

  /**
   * Добавляет товар в личный кабинет и перенаправляет обратно на его страницу.
   * @param p вошедший пользователь
   * @param id уникальный номер записи
   * @param flash сообщения, доступные после перенаправления браузера
   */
  @PostMapping("/account/products/{id}")
  public String add(Principal p, @PathVariable UUID id, RedirectAttributes flash) {
    accounts.addSelected(p.getName(), id);
    flash.addFlashAttribute("message", "Товар сохранён в личном кабинете");
    return "redirect:/account";
  }

  /** Добавляет отмеченные товары и возвращает в тот же раздел каталога. */
  @PostMapping("/account/products/add-selected")
  public String addSelected(
      Principal p,
      @RequestParam(name = "productIds", required = false) java.util.List<UUID> productIds,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) org.example.aav.model.Category category,
      @RequestParam(defaultValue = "0") int page,
      RedirectAttributes flash) {
    var ids = productIds == null ? java.util.List.<UUID>of() : productIds;
    int added = accounts.addSelectedProducts(p.getName(), ids);
    flash.addFlashAttribute("message", ids.isEmpty() ? "Выберите товары для добавления"
        : added == 0 ? "Все выбранные товары уже в корзине" : "Добавлено в корзину: " + added);
    var target = org.springframework.web.util.UriComponentsBuilder.fromPath("/products")
        .queryParam("q", q);
    if (category != null) target.queryParam("category", category);
    target.queryParam("page", Math.max(0, page)).fragment("catalog");
    return "redirect:" + target.build().encode().toUriString();
  }

  /**
   * Удаляет товар из личного кабинета и перенаправляет обратно.
   * @param p вошедший пользователь
   * @param id уникальный номер записи
   */
  @PostMapping("/account/products/{id}/remove")
  public String remove(Principal p, @PathVariable UUID id) {
    accounts.removeSelected(p.getName(), id);
    return "redirect:/account";
  }

  /** Удаляет отмеченные товары одним действием. */
  @PostMapping("/account/products/remove-selected")
  public String removeSelected(
      Principal p,
      @RequestParam(name = "productIds", required = false) java.util.List<UUID> productIds,
      RedirectAttributes flash) {
    long removed = accounts.removeSelectedProducts(
        p.getName(), productIds == null ? java.util.List.of() : productIds);
    flash.addFlashAttribute("message", removed == 0
        ? "Выберите товары для удаления" : "Удалено товаров: " + removed);
    return "redirect:/account";
  }

  /**
   * Проверяет форму отзыва; при ошибках снова показывает её, иначе сохраняет отзыв.
   * @param p вошедший пользователь
   * @param form данные формы, которые нужно проверить или заполнить
   * @param errors ошибки заполнения и проверки формы
   * @param model данные, которые Spring передаст HTML-шаблону
   * @param flash сообщения, доступные после перенаправления браузера
   */
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

  /**
   * Показывает личный кабинет с формой редактирования выбранного отзыва.
   * @param principal сведения о пользователе, который вошёл в систему
   * @param id уникальный номер записи
   * @param model данные, которые Spring передаст HTML-шаблону
   */
  @GetMapping("/account/reviews/{id}/edit")
  public String editReview(Principal principal, @PathVariable UUID id, Model model) {
    populate(principal, model);
    model.addAttribute("reviewForm", accounts.reviewForEditing(principal.getName(), id));
    return "account";
  }

  /**
   * Удаляет отзыв только тогда, когда он принадлежит текущему пользователю.
   * @param p вошедший пользователь
   * @param id уникальный номер записи
   */
  @PostMapping("/account/reviews/{id}/remove")
  public String removeReview(Principal p, @PathVariable UUID id) {
    accounts.removeReview(p.getName(), id);
    return "redirect:/account";
  }

  /**
   * Загружает фотографию в профиль или возвращает её байты браузеру.
   * @param p вошедший пользователь
   * @param file загруженный пользователем файл
   * @param flash сообщения, доступные после перенаправления браузера
   */
  @PostMapping("/account/avatar")
  public String avatar(Principal p, @RequestParam MultipartFile file, RedirectAttributes flash) {
    try {
      accounts.uploadAvatar(p.getName(), file);
      flash.addFlashAttribute("message", "Фотография обновлена");
    // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
    } catch (IllegalArgumentException ex) {
      flash.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/account";
  }

  /**
   * Удаляет фотографию из профиля пользователя.
   * @param p вошедший пользователь
   */
  @PostMapping("/account/avatar/remove")
  public String removeAvatar(Principal p) {
    accounts.removeAvatar(p.getName());
    return "redirect:/account";
  }

  /**
   * Загружает фотографию в профиль или возвращает её байты браузеру.
   * @param p вошедший пользователь
   */
  @GetMapping("/account/avatar")
  @ResponseBody
  public ResponseEntity<byte[]> avatar(Principal p) {
    // Учётная запись пользователя.
    var user = accounts.user(p.getName());
    if (!user.isHasAvatar()) return ResponseEntity.notFound().build();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.IMAGE_JPEG)
        .body(user.getAvatar());
  }
}
