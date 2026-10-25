package org.example.aav.model;

import jakarta.persistence.*;

/** Этап 2: описываем аккаунт для хранения профиля, контактов и хеша пароля. Учебная последовательность. */
@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {
  // Логин пользователя для входа.
  @Column(nullable = false, unique = true, length = 50)
  private String username;

  // Имя пользователя для показа на странице.
  @Column(nullable = false, length = 100)
  private String displayName;

  // Хеш пароля: результат одностороннего преобразования для проверки при входе.
  @Column(nullable = false, length = 100)
  private String passwordHash;

  // Роль пользователя, определяющая права доступа.
  @Column(nullable = false, length = 20)
  private String role = "USER";

  // Фамилия пользователя.
  @Column(length = 100)
  private String lastName;
  // Контактный телефон.
  @Column(length = 30)
  private String phone;
  // Адрес электронной почты.
  @Column(length = 254)
  private String email;

  /**
   * Возвращает фамилию пользователя.
   */
  public String getLastName() { return lastName; }
  /**
   * Возвращает контактный телефон.
   */
  public String getPhone() { return phone; }
  /**
   * Возвращает адрес электронной почты.
   */
  public String getEmail() { return email; }
  /**
   * Сохраняет фамилию, телефон и email в объекте пользователя.
   * @param lastName фамилия пользователя
   * @param phone контактный телефон
   * @param email адрес электронной почты
   */
  public void setContacts(String lastName, String phone, String email) {
    this.lastName = lastName;
    this.phone = phone;
    this.email = email;
  }

  // Байты фотографии профиля.
  @Basic(fetch = FetchType.LAZY)
  @Column(columnDefinition = "bytea")
  private byte[] avatar;

  // Тип файла фотографии для ответа браузеру.
  @Column(length = 30)
  private String avatarContentType;

  /**
   * Пустой конструктор нужен JPA для загрузки записи из базы.
   */
  protected UserAccount() {}

  /**
   * Создаёт объект и заполняет его начальными данными.
   * @param username логин пользователя для входа
   * @param displayName имя пользователя для показа на странице
   * @param passwordHash хеш пароля: результат одностороннего преобразования для проверки при входе
   */
  public UserAccount(String username, String displayName, String passwordHash) {
    this.username = username;
    this.displayName = displayName;
    this.passwordHash = passwordHash;
  }

  /**
   * Возвращает логин пользователя для входа.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Возвращает имя пользователя для показа на странице.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Возвращает хеш пароля: результат одностороннего преобразования для проверки при входе.
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Записывает хеш пароля: результат одностороннего преобразования для проверки при входе.
   * @param hash новый хеш пароля
   */
  public void setPasswordHash(String hash) { passwordHash = hash; }

  /**
   * Возвращает роль пользователя, определяющую права доступа.
   */
  public String getRole() {
    return role;
  }

  /**
   * Возвращает байты фотографии профиля.
   */
  public byte[] getAvatar() {
    return avatar;
  }

  /**
   * Возвращает тип файла фотографии для ответа браузеру.
   */
  public String getAvatarContentType() {
    return avatarContentType;
  }

  /**
   * Проверяет, загружена ли фотография профиля.
   */
  public boolean isHasAvatar() {
    return avatar != null;
  }

  /**
   * Заменяет фотографию и её тип; значения null удаляют фотографию.
   * @param bytes байты фотографии
   * @param type тип содержимого фотографии
   */
  public void setAvatar(byte[] bytes, String type) {
    avatar = bytes;
    avatarContentType = type;
  }

  /**
   * Записывает имя пользователя для показа на странице.
   * @param name название товара
   */
  public void setDisplayName(String name) {
    displayName = name;
  }
}
