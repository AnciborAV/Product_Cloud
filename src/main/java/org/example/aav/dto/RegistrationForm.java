package org.example.aav.dto;

import jakarta.validation.constraints.*;

public class RegistrationForm {
  @NotBlank(message = "Введите логин")
  @Pattern(
      regexp = "[a-zA-Z0-9_]{3,50}",
      message = "Логин: 3–50 латинских букв, цифр или символов _")
  private String username;

  @NotBlank(message = "Введите ваше имя")
  @Size(max = 100)
  private String displayName;

  @NotBlank(message = "Введите фамилию")
  @Size(max = 100)
  private String lastName;

  @NotBlank(message = "Введите телефон")
  @Pattern(regexp = "[+0-9() -]{7,30}", message = "Введите корректный телефон")
  private String phone;

  @NotBlank(message = "Введите email")
  @Email(message = "Введите корректный email")
  @Size(max = 254)
  private String email;

  public String getLastName() { return lastName; }
  public void setLastName(String value) { lastName = value; }
  public String getPhone() { return phone; }
  public void setPhone(String value) { phone = value; }
  public String getEmail() { return email; }
  public void setEmail(String value) { email = value; }

  @NotBlank
  @Size(min = 12, max = 64, message = "Пароль: от 12 до 64 символов")
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String v) {
    username = v;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String v) {
    displayName = v;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String v) {
    password = v;
  }
}
