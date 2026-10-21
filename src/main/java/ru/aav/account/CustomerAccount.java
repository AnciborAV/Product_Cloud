package ru.aav.account;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Личная учётная запись. Пароль хранится только как стойкий хеш; фото — нормализованный JPEG.
 * {@code @Entity} объявляет JPA-сущность, связанную со строкой таблицы. {@code @Table} задаёт
 * таблицу и её ограничения/индексы.
 */
@Entity
@Table(name = "customer_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class CustomerAccount {
    /**
     * Идентификатор записи; объект получает его при сохранении. {@code @Id} помечает первичный
     * ключ. {@code @GeneratedValue} поручает генерацию идентификатора базе/ORM согласно выбранной
     * стратегии.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Нормализованный email; служит логином. {@code @Column} задаёт имя, допустимость NULL, длину
     * или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 254)
    private String email;

    /**
     * Хеш пароля; исходный пароль не хранится. {@code @Column} задаёт имя, допустимость NULL, длину
     * или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 255)
    private String passwordHash;

    /**
     * Имя пользователя для профиля и приветствия. {@code @Column} задаёт имя, допустимость NULL,
     * длину или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 80)
    private String displayName;

    /**
     * Контактный телефон. {@code @Column} задаёт имя, допустимость NULL, длину или точность
     * столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 40)
    private String phone = "";

    /**
     * Контактный адрес. {@code @Column} задаёт имя, допустимость NULL, длину или точность столбца;
     * это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 500)
    private String address = "";

    /**
     * Момент создания записи в UTC. {@code @Column} задаёт имя, допустимость NULL, длину или
     * точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false)
    private Instant createdAt;

    // byte[] без @Lob отображается в bytea в PostgreSQL и varbinary в H2.
    /**
     * Байты нормализованного JPEG; null означает отсутствие фотографии. {@code @Column} задаёт имя,
     * допустимость NULL, длину или точность столбца; это не проверка HTML-формы.
     */
    @Column(length = 2097152)
    private byte[] avatar;

    /** Конструктор для JPA: ORM создаёт экземпляр и заполняет поля при чтении из базы. */
    protected CustomerAccount() {}

    /**
     * Создаёт учётную запись с уже вычисленным хешем пароля, датой регистрации и защитной копией
     * фото.
     */
    public CustomerAccount(String email, String passwordHash, String displayName, byte[] avatar) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = Instant.now();
        this.avatar = avatar == null ? null : avatar.clone();
    }

    /** Очищает пробелы по краям контактных данных и обновляет профиль. */
    public void updateProfile(String displayName, String phone, String address) {
        this.displayName = displayName.strip();
        this.phone = phone.strip();
        this.address = address.strip();
    }

    /** Заменяет уже вычисленный хеш; открытый пароль передавать сюда нельзя. */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Сохраняет копию массива фотографии, чтобы вызывающий код не мог незаметно изменить её
     * содержимое.
     */
    public void setAvatar(byte[] avatar) {
        this.avatar = avatar == null ? null : avatar.clone();
    }

    /** Возвращает идентификатор записи. */
    public Long getId() {
        return id;
    }

    /** Возвращает нормализованный email. */
    public String getEmail() {
        return email;
    }

    /** Возвращает хеш пароля для внутренней проверки; его нельзя выводить пользователю. */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** Возвращает имя пользователя. */
    public String getDisplayName() {
        return displayName;
    }

    /** Возвращает контактный телефон. */
    public String getPhone() {
        return phone;
    }

    /** Возвращает контактный адрес. */
    public String getAddress() {
        return address;
    }

    /** Возвращает момент создания записи. */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Возвращает защитную копию массива фото либо null. */
    public byte[] getAvatar() {
        return avatar == null ? null : avatar.clone();
    }

    /** Проверяет наличие фотографии без создания копии её массива. */
    public boolean hasAvatar() {
        return avatar != null;
    }
}
