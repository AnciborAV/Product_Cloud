package ru.aav.account;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Регистрация и изменения профиля выполняются транзакционно, независимо от HTML-контроллера.
 * {@code @Validated} включает проверку аргументов публичных методов при обращении через
 * Spring-прокси. {@code @Service} регистрирует сервис как компонент Spring. {@code @Transactional}
 * задаёт границу транзакции при вызове через Spring-прокси; readOnly — оптимизация чтения,
 * MANDATORY требует уже открытую транзакцию.
 */
@Validated
@Service
@Transactional(readOnly = true)
public class AccountService {
    /** Зависимость для поиска и изменения учётных записей. */
    private final AccountRepository accounts;

    /** Кодировщик для создания и проверки хешей паролей. */
    private final PasswordEncoder passwords;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public AccountService(AccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
    }

    /**
     * Удаляет пробелы по краям и приводит email к нижнему регистру независимо от локали компьютера.
     */
    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Проверяет совпадение паролей и уникальность email, хеширует пароль и сохраняет аккаунт;
     * уникальное ограничение БД защищает также от одновременной регистрации. {@code @Transactional}
     * задаёт границу транзакции при вызове через Spring-прокси; readOnly — оптимизация чтения,
     * MANDATORY требует уже открытую транзакцию. {@code @Valid} запускает проверку ограничений
     * полей переданного объекта.
     */
    @Transactional
    public AccountView register(@Valid AccountForms.Registration form, byte[] avatar) {
        // Нормализованный логин для проверки или регистрации.
        String email = normalizeEmail(form.email());
        if (!form.password().equals(form.confirmation()))
            throw new IllegalArgumentException("Пароли не совпадают");
        if (accounts.existsByEmail(email))
            throw new IllegalArgumentException("Этот email уже зарегистрирован. Войдите в кабинет");
        return AccountView.from(
                accounts.saveAndFlush(
                        new CustomerAccount(
                                email,
                                passwords.encode(form.password()),
                                form.displayName().strip(),
                                avatar)));
    }

    /**
     * Находит аккаунт по нормализованному логину; отсутствие записи означает, что запрос не
     * авторизован.
     */
    public CustomerAccount require(String email) {
        return accounts.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** Преобразует аккаунт в безопасный DTO без хеша пароля и содержимого фотографии. */
    public AccountView view(String email) {
        return AccountView.from(require(email));
    }

    /**
     * Читает защитную копию фотографии или заменяет её у владельца; null означает удаление фото.
     */
    public byte[] avatar(String email) {
        return require(email).getAvatar();
    }

    /**
     * Обновляет имя, телефон и адрес управляемой JPA-сущности; Hibernate сохранит изменения при
     * завершении транзакции. {@code @Transactional} задаёт границу транзакции при вызове через
     * Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     * {@code @Valid} запускает проверку ограничений полей переданного объекта.
     */
    @Transactional
    public void update(String email, @Valid AccountForms.Profile form) {
        require(email).updateProfile(form.displayName(), form.phone(), form.address());
    }

    /**
     * Читает защитную копию фотографии или заменяет её у владельца; null означает удаление фото.
     * {@code @Transactional} задаёт границу транзакции при вызове через Spring-прокси; readOnly —
     * оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     */
    @Transactional
    public void avatar(String email, byte[] image) {
        require(email).setAvatar(image);
    }

    /**
     * Проверяет старый пароль и подтверждение нового, затем заменяет хеш; исходные пароли в базу не
     * записываются. {@code @Transactional} задаёт границу транзакции при вызове через
     * Spring-прокси; readOnly — оптимизация чтения, MANDATORY требует уже открытую транзакцию.
     * {@code @Valid} запускает проверку ограничений полей переданного объекта.
     */
    @Transactional
    public void password(String email, @Valid AccountForms.Password form) {
        // Аккаунт текущего владельца; его идентификатор ограничивает доступ к личным данным.
        var account = require(email);
        if (!passwords.matches(form.currentPassword(), account.getPasswordHash()))
            throw new IllegalArgumentException("Текущий пароль указан неверно");
        if (!form.newPassword().equals(form.confirmation()))
            throw new IllegalArgumentException("Новые пароли не совпадают");
        account.setPasswordHash(passwords.encode(form.newPassword()));
    }
}
