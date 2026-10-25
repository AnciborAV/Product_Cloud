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

@Service
@org.springframework.validation.annotation.Validated
@Transactional(readOnly = true)
public class AccountService {
  private final UserRepository users;
  private final ProductRepository products;
  private final SelectedProductRepository selected;
  private final ReviewRepository reviews;
  private final PasswordEncoder encoder;
  private final AvatarProcessor avatarProcessor;
  private final SelectionMailService selectionMail;

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

  @Transactional
  public void register(@jakarta.validation.Valid RegistrationForm form) {
    String username = form.getUsername().toLowerCase(Locale.ROOT);
    if (users.findByUsername(username).isPresent())
      throw new IllegalArgumentException("Этот логин уже занят");
    if (form.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException("Пароль должен занимать не более 72 байт UTF-8");
    var account = new UserAccount(username, form.getDisplayName().strip(), encoder.encode(form.getPassword()));
    account.setContacts(form.getLastName().strip(), form.getPhone().strip(), form.getEmail().strip());
    users.saveAndFlush(account);
  }

  @Transactional
  public void updateContacts(String username, @jakarta.validation.Valid ContactForm form) {
    var account = lock(username);
    account.setDisplayName(form.displayName().strip());
    account.setContacts(form.lastName().strip(), form.phone().strip(), form.email().strip());
    selectionMail.enqueue(account, selected.findByUserUsernameOrderByCreatedAtDesc(username));
  }

  @Transactional
  public void emailOrder(String username) {
    var account = lock(username);
    if (account.getEmail() == null || account.getEmail().isBlank())
      throw new IllegalArgumentException("Укажите email в контактах и сохраните его.");
    var items = selected.findByUserUsernameOrderByCreatedAtDesc(username);
    if (items.isEmpty()) throw new IllegalArgumentException("Добавьте товары в заказ.");
    selectionMail.enqueue(account, items);
  }

  public UserAccount user(String username) {
    return users
        .findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private UserAccount lock(String username) {
    return users
        .lockByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public List<Product> selected(String username) {
    return selected.findByUserUsernameOrderByCreatedAtDesc(username).stream()
        .map(s -> s.getProduct().toProduct())
        .toList();
  }

  public List<Review> reviews(String username) {
    return reviews.findByUserUsernameOrderByCreatedAtDesc(username);
  }

  @Transactional
  public void addSelected(String username, UUID productId) {
    var user = lock(username);
    var product =
        products
            .findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!selected.existsByUserIdAndProductId(user.getId(), productId)) {
      selected.saveAndFlush(new SelectedProduct(user, product));
      selectionMail.enqueue(user, selected.findByUserUsernameOrderByCreatedAtDesc(username));
    }
  }

  @Transactional
  public void removeSelected(String username, UUID productId) {
    var user = lock(username);
    if (selected.existsByUserIdAndProductId(user.getId(), productId)) {
      selected.deleteByUserIdAndProductId(user.getId(), productId);
      selected.flush();
      selectionMail.enqueue(user, selected.findByUserUsernameOrderByCreatedAtDesc(username));
    }
  }

  @Transactional
  public void saveReview(String username, @jakarta.validation.Valid ReviewForm form) {
    var user = lock(username);
    var product =
        products
            .findById(form.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var review =
        reviews
            .findByUserIdAndProductId(user.getId(), product.getId())
            .orElseGet(() -> new Review(user, product, form.getRating(), form.getText().strip()));
    review.update(form.getRating(), form.getText().strip());
    reviews.save(review);
  }

  public ReviewForm reviewForEditing(String username, UUID id) {
    var review =
        reviews(username).stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var form = new ReviewForm();
    form.setProductId(review.getProduct().getId());
    form.setRating(review.getRating());
    form.setText(review.getText());
    return form;
  }

  @Transactional
  public void removeReview(String username, UUID id) {
    reviews.deleteByIdAndUserId(id, lock(username).getId());
  }

  @Transactional
  public void uploadAvatar(String username, MultipartFile file) {
    byte[] avatar = avatarProcessor.process(file);
    lock(username).setAvatar(avatar, "image/jpeg");
  }

  @Transactional
  public void removeAvatar(String username) {
    lock(username).setAvatar(null, null);
  }
}
