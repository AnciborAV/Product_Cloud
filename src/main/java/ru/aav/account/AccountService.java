package ru.aav.account;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

/** Регистрация и изменения профиля выполняются транзакционно, независимо от HTML-контроллера. */
@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    public AccountService(AccountRepository accounts, PasswordEncoder passwords) {
        this.accounts = accounts; this.passwords = passwords;
    }
    public static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    @Transactional
    public AccountView register(AccountForms.Registration form, byte[] avatar) {
        String email = normalizeEmail(form.email());
        if (!form.password().equals(form.confirmation())) throw new IllegalArgumentException("Пароли не совпадают");
        if (accounts.existsByEmail(email)) throw new IllegalArgumentException("Этот email уже зарегистрирован. Войдите в кабинет");
        return AccountView.from(accounts.saveAndFlush(new CustomerAccount(email, passwords.encode(form.password()),
                form.displayName().strip(), avatar)));
    }
    public CustomerAccount require(String email) {
        return accounts.findByEmail(normalizeEmail(email)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    public AccountView view(String email) { return AccountView.from(require(email)); }
    public byte[] avatar(String email) { return require(email).getAvatar(); }
    @Transactional
    public void update(String email, AccountForms.Profile form) {
        require(email).updateProfile(form.displayName(), form.phone(), form.address());
    }
    @Transactional
    public void avatar(String email, byte[] image) { require(email).setAvatar(image); }
    @Transactional
    public void password(String email, AccountForms.Password form) {
        var a = require(email);
        if (!passwords.matches(form.currentPassword(), a.getPasswordHash()))
            throw new IllegalArgumentException("Текущий пароль указан неверно");
        if (!form.newPassword().equals(form.confirmation())) throw new IllegalArgumentException("Новые пароли не совпадают");
        a.setPasswordHash(passwords.encode(form.newPassword()));
    }
}
