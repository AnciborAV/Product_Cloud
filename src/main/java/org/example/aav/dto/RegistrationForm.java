package org.example.aav.dto;

import jakarta.validation.constraints.*;

/** Этап 3: описываем форму регистрации, чтобы проверить данные до создания аккаунта. Учебная последовательность. */
public class RegistrationForm {
  // Логин пользователя для входа.
  @NotBlank(message = "Введите логин")
  @Pattern(
      regexp = "[a-zA-Z0-9_]{3,50}",
      message = "Логин: 3–50 латинских букв, цифр или символов _")
  private String username;

  // Имя пользователя для показа на странице.
  @NotBlank(message = "Введите ваше имя")
  @Size(max = 100)
  private String displayName;

  // Фамилия пользователя.
  @NotBlank(message = "Введите фамилию")
  @Size(max = 100)
  private String lastName;

  // Контактный телефон.
  @NotBlank(message = "Введите телефон")
  @Pattern(regexp = "[+0-9() -]{7,30}", message = "Введите корректный телефон")
  private String phone;

  // Адрес электронной почты.
  @NotBlank(message = "Введите email")
  @Email(message = "Введите корректный email")
  @Size(max = 254)
  private String email;

  /**
   * Возвращает фамилию пользователя.
   */
  public String getLastName() { return lastName; }
  /**
   * Записывает фамилию пользователя.
   * @param value новая фамилия
   */
  public void setLastName(String value) { lastName = value; }
  /**
   * Возвращает контактный телефон.
   */
  public String getPhone() { return phone; }
  /**
   * Записывает контактный телефон.
   * @param value новый телефон
   */
  public void setPhone(String value) { phone = value; }
  /**
   * Возвращает адрес электронной почты.
   */
  public String getEmail() { return email; }
  /**
   * Записывает адрес электронной почты.
   * @param value новый email
   */
  public void setEmail(String value) { email = value; }

  // Пароль, введённый пользователем.
  @NotBlank
  @Size(min = 12, max = 64, message = "Пароль: от 12 до 64 символов")
  private String password;

  /**
   * Возвращает логин пользователя для входа.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Записывает логин пользователя для входа.
   * @param v новый логин
   */
  public void setUsername(String v) {
    username = v;
  }

  /**
   * Возвращает имя пользователя для показа на странице.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Записывает имя пользователя для показа на странице.
   * @param v новое отображаемое имя
   */
  public void setDisplayName(String v) {
    displayName = v;
  }

  /**
   * Возвращает пароль, введённый пользователем.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Записывает пароль, введённый пользователем.
   * @param v пароль из формы
   */
  public void setPassword(String v) {
    password = v;
  }
}
