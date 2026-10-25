package org.example.aav.service;

import java.util.*;
import org.example.aav.dto.*;
import org.example.aav.model.*;
import org.example.aav.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Этап 5: собираем действия личного кабинета и выполняем изменения базы в транзакциях. Учебная последовательность. */
@Service
@org.springframework.validation.annotation.Validated
@Transactional(readOnly = true)
public class AccountService {
  // Доступ к учётным записям в базе данных.
  private final UserRepository users;
  // Доступ к таблице товаров в базе.
  private final ProductRepository products;
  // Доступ к сохранённым товарам пользователя.
  private final SelectedProductRepository selected;
  // Отзывы пользователя или доступ к ним в базе.
  private final ReviewRepository reviews;
  // Средство создания и проверки хешей паролей.
  private final PasswordEncoder encoder;
  // Проверка и уменьшение загруженной фотографии.
  private final AvatarProcessor avatarProcessor;
  // Подготовка писем со списком выбранных товаров.
  private final SelectionMailService selectionMail;

  /**
   * Создаёт объект и получает необходимые зависимости через конструктор; Spring передаёт их при создании.
   * @param users доступ к учётным записям в базе данных
   * @param products репозиторий для чтения и сохранения товаров в базе
   * @param selected доступ к сохранённым товарам пользователя
   * @param reviews отзывы пользователя или доступ к ним в базе
   * @param encoder средство создания и проверки хешей паролей
   * @param avatarProcessor проверка и уменьшение загруженной фотографии
   * @param selectionMail подготовка писем со списком выбранных товаров
   */
  public AccountService(
      UserRepository users,
      ProductRepository products,
      SelectedProductRepository selected,
      ReviewRepository reviews,
      PasswordEncoder encoder,
      AvatarProcessor avatarProcessor, SelectionMailService selectionMail) {
    this.users = users;
    this.products = products;
    this.selected = selected;
    this.reviews = reviews;
    this.encoder = encoder;
    this.avatarProcessor = avatarProcessor;
    this.selectionMail = selectionMail;
  }

  /**
   * Показывает форму регистрации или проверяет и сохраняет нового пользователя.
   * @param form данные формы, которые нужно проверить или заполнить
   */
  @Transactional
  public void register(@jakarta.validation.Valid RegistrationForm form) {
    // Логин пользователя для входа.
    String username = form.getUsername().toLowerCase(Locale.ROOT);
    if (users.findByUsername(username).isPresent())
      throw new IllegalArgumentException("Этот логин уже занят");
    if (form.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException("Пароль должен занимать не более 72 байт UTF-8");
    // Значение для текущей операции.
    var account = new UserAccount(username, form.getDisplayName().strip(), encoder.encode(form.getPassword()));
    account.setContacts(form.getLastName().strip(), form.getPhone().strip(), form.getEmail().strip());
    users.saveAndFlush(account);
  }

  /**
   * Обновляет контакты пользователя и ставит письмо с текущим списком товаров в очередь.
   * @param username логин пользователя для входа
   * @param form данные формы, которые нужно проверить или заполнить
   */
  @Transactional
  public void updateContacts(String username, @jakarta.validation.Valid ContactForm form) {
    // Значение для текущей операции.
    var account = lock(username);
    account.setDisplayName(form.displayName().strip());
    account.setContacts(form.lastName().strip(), form.phone().strip(), form.email().strip());
    selectionMail.enqueue(account, selected.findByUserUsernameOrderByCreatedAtDesc(username));
  }

  /**
   * Ставит письмо с выбранными товарами в очередь; сообщает об отсутствии email или товаров.
   * @param username логин пользователя для входа
   */
  @Transactional
  public void emailOrder(String username) {
    // Значение для текущей операции.
    var account = lock(username);
    if (account.getEmail() == null || account.getEmail().isBlank())
      throw new IllegalArgumentException("Укажите email в контактах и сохраните его.");
    // Список выбранных товаров.
    var items = selected.findByUserUsernameOrderByCreatedAtDesc(username);
    if (items.isEmpty()) throw new IllegalArgumentException("Добавьте товары в заказ.");
    selectionMail.enqueue(account, items);
  }

  /**
   * Находит пользователя по логину; если его нет, сообщает об ошибке 404.
   * @param username логин пользователя для входа
   */
  public UserAccount user(String username) {
    return users
        .findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /**
   * Находит и блокирует запись пользователя до конца операции, чтобы изменения не мешали друг другу.
   * @param username логин пользователя для входа
   */
  private UserAccount lock(String username) {
    return users
        .lockByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /**
   * Возвращает выбранные пользователем товары, начиная с последних добавленных.
   * @param username логин пользователя для входа
   */
  public List<Product> selected(String username) {
    return selected.findByUserUsernameOrderByCreatedAtDesc(username).stream()
        // s — очередной выбранный товар.
        .map(s -> s.getProduct().toProduct())
        .toList();
  }

  /**
   * Загружает отзывы для пользователя или страницы отзывов.
   * @param username логин пользователя для входа
   */
  public List<Review> reviews(String username) {
    return reviews.findByUserUsernameOrderByCreatedAtDesc(username);
  }

  /**
   * Добавляет товар в список один раз и ставит обновлённый список в очередь писем.
   * @param username логин пользователя для входа
   * @param productId уникальный номер товара
   */
  @Transactional
  public void addSelected(String username, UUID productId) {
    // Учётная запись пользователя.
    var user = lock(username);
    // Товар, с которым выполняется действие.
    var product =
        products
            .findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!selected.existsByUserIdAndProductId(user.getId(), productId)) {
      selected.saveAndFlush(new SelectedProduct(user, product));
      selectionMail.enqueue(user, selected.findByUserUsernameOrderByCreatedAtDesc(username));
    }
  }

  /** Добавляет подборку одной транзакцией и готовит одно письмо с итоговым списком. */
  @Transactional
  public int addSelectedProducts(String username, Collection<UUID> productIds) {
    if (productIds.isEmpty()) return 0;
    var user = lock(username);
    var ids = new HashSet<>(productIds);
    var found = products.findAllById(ids);
    if (found.size() != ids.size())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    int added = 0;
    for (var product : found) {
      if (!selected.existsByUserIdAndProductId(user.getId(), product.getId())) {
        selected.save(new SelectedProduct(user, product));
        added++;
      }
    }
    if (added > 0) {
      selected.flush();
      selectionMail.enqueue(user, selected.findByUserUsernameOrderByCreatedAtDesc(username));
    }
    return added;
  }

  /**
   * Удаляет выбранный товар и готовит письмо с обновлённым списком.
   * @param username логин пользователя для входа
   * @param productId уникальный номер товара
   */
  @Transactional
  public void removeSelected(String username, UUID productId) {
    // Учётная запись пользователя.
    var user = lock(username);
    if (selected.existsByUserIdAndProductId(user.getId(), productId)) {
      selected.deleteByUserIdAndProductId(user.getId(), productId);
      selected.flush();
      selectionMail.enqueue(user, selected.findByUserUsernameOrderByCreatedAtDesc(username));
    }
  }

  /** Удаляет отмеченные товары только из подборки текущего пользователя. */
  @Transactional
  public long removeSelectedProducts(String username, Collection<UUID> productIds) {
    if (productIds.isEmpty()) return 0;
    var user = lock(username);
    long removed = selected.deleteByUserIdAndProductIdIn(user.getId(), new HashSet<>(productIds));
    if (removed > 0) {
      selected.flush();
      selectionMail.enqueue(user, selected.findByUserUsernameOrderByCreatedAtDesc(username));
    }
    return removed;
  }

  /**
   * Создаёт отзыв или обновляет существующий отзыв этого пользователя о товаре.
   * @param username логин пользователя для входа
   * @param form данные формы, которые нужно проверить или заполнить
   */
  @Transactional
  public void saveReview(String username, @jakarta.validation.Valid ReviewForm form) {
    // Учётная запись пользователя.
    var user = lock(username);
    // Товар, с которым выполняется действие.
    var product =
        products
            .findById(form.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    // Значение для текущей операции.
    var review =
        reviews
            .findByUserIdAndProductId(user.getId(), product.getId())
            .orElseGet(() -> new Review(user, product, form.getRating(), form.getText().strip()));
    review.update(form.getRating(), form.getText().strip());
    reviews.save(review);
  }

  /**
   * Заполняет форму данными собственного отзыва пользователя для редактирования.
   * @param username логин пользователя для входа
   * @param id уникальный номер записи
   */
  public ReviewForm reviewForEditing(String username, UUID id) {
    // Значение для текущей операции.
    var review =
        reviews(username).stream()
            // r — очередной отзыв для проверки его номера.
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    // Данные формы, которые нужно проверить или заполнить.
    var form = new ReviewForm();
    form.setProductId(review.getProduct().getId());
    form.setRating(review.getRating());
    form.setText(review.getText());
    return form;
  }

  /**
   * Удаляет отзыв только тогда, когда он принадлежит текущему пользователю.
   * @param username логин пользователя для входа
   * @param id уникальный номер записи
   */
  @Transactional
  public void removeReview(String username, UUID id) {
    reviews.deleteByIdAndUserId(id, lock(username).getId());
  }

  /**
   * Проверяет фотографию, уменьшает её и сохраняет в профиле.
   * @param username логин пользователя для входа
   * @param file загруженный пользователем файл
   */
  @Transactional
  public void uploadAvatar(String username, MultipartFile file) {
    // Байты фотографии профиля.
    byte[] avatar = avatarProcessor.process(file);
    lock(username).setAvatar(avatar, "image/jpeg");
  }

  /**
   * Удаляет фотографию из профиля пользователя.
   * @param username логин пользователя для входа
   */
  @Transactional
  public void removeAvatar(String username) {
    lock(username).setAvatar(null, null);
  }
}
