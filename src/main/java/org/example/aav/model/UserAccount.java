package org.example.aav.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {
  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(nullable = false, length = 100)
  private String displayName;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 20)
  private String role = "USER";

  @Column(length = 100)
  private String lastName;
  @Column(length = 30)
  private String phone;
  @Column(length = 254)
  private String email;

  public String getLastName() { return lastName; }
  public String getPhone() { return phone; }
  public String getEmail() { return email; }
  public void setContacts(String lastName, String phone, String email) {
    this.lastName = lastName;
    this.phone = phone;
    this.email = email;
  }

  @Basic(fetch = FetchType.LAZY)
  @Column(columnDefinition = "bytea")
  private byte[] avatar;

  @Column(length = 30)
  private String avatarContentType;

  protected UserAccount() {}

  public UserAccount(String username, String displayName, String passwordHash) {
    this.username = username;
    this.displayName = displayName;
    this.passwordHash = passwordHash;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String hash) { passwordHash = hash; }

  public String getRole() {
    return role;
  }

  public byte[] getAvatar() {
    return avatar;
  }

  public String getAvatarContentType() {
    return avatarContentType;
  }

  public boolean isHasAvatar() {
    return avatar != null;
  }

  public void setAvatar(byte[] bytes, String type) {
    avatar = bytes;
    avatarContentType = type;
  }

  public void setDisplayName(String name) {
    displayName = name;
  }
}
