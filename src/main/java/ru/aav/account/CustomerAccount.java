package ru.aav.account;

import jakarta.persistence.*;
import java.time.Instant;

/** Личная учётная запись. Пароль хранится только как стойкий хеш; фото — нормализованный JPEG. */
@Entity
@Table(name = "customer_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class CustomerAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false, length = 80)
    private String displayName;
    @Column(nullable = false, length = 40)
    private String phone = "";
    @Column(nullable = false, length = 500)
    private String address = "";
    @Column(nullable = false)
    private Instant createdAt;
    // byte[] без @Lob отображается в bytea в PostgreSQL и varbinary в H2.
    @Column(length = 2097152)
    private byte[] avatar;

    protected CustomerAccount() { }
    public CustomerAccount(String email, String passwordHash, String displayName, byte[] avatar) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = Instant.now();
        this.avatar = avatar;
    }
    public void updateProfile(String displayName, String phone, String address) {
        this.displayName = displayName.strip();
        this.phone = phone.strip();
        this.address = address.strip();
    }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setAvatar(byte[] avatar) { this.avatar = avatar; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public Instant getCreatedAt() { return createdAt; }
    public byte[] getAvatar() { return avatar; }
}
