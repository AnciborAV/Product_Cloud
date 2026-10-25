package org.example.aav.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.example.aav.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {
  private final UserRepository users;
  private final JdbcTemplate jdbc;
  private final PasswordEncoder encoder;
  private final boolean mailEnabled;
  private final SecureRandom random = new SecureRandom();

  public PasswordChangeService(UserRepository users, JdbcTemplate jdbc, PasswordEncoder encoder,
      @Value("${app.mail.enabled:false}") boolean mailEnabled) {
    this.users = users;
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.mailEnabled = mailEnabled;
  }

  @Transactional
  public void request(String username, String currentPassword, String newPassword, String repeatPassword) {
    if (!mailEnabled) throw new IllegalArgumentException("Отправка почты отключена. Обратитесь к администратору.");
    var user = users.lockByUsername(username).orElseThrow();
    if (user.getEmail() == null || user.getEmail().isBlank())
      throw new IllegalArgumentException("Сначала сохраните email в контактах.");
    if (currentPassword == null || !encoder.matches(currentPassword, user.getPasswordHash()))
      throw new IllegalArgumentException("Текущий пароль указан неверно.");
    if (newPassword == null || newPassword.length() < 10 || newPassword.length() > 72
        || newPassword.getBytes(StandardCharsets.UTF_8).length > 72 || newPassword.isBlank())
      throw new IllegalArgumentException("Новый пароль: от 10 символов и не более 72 байт UTF-8.");
    if (!newPassword.equals(repeatPassword)) throw new IllegalArgumentException("Новые пароли не совпадают.");
    if (encoder.matches(newPassword, user.getPasswordHash()))
      throw new IllegalArgumentException("Новый пароль должен отличаться от текущего.");
    Instant now = Instant.now();
    var requests = jdbc.queryForList("SELECT requested_at FROM password_changes WHERE user_id=?", user.getId());
    if (!requests.isEmpty()) {
      var requested = jdbc.queryForObject("SELECT requested_at FROM password_changes WHERE user_id=?",
          java.time.OffsetDateTime.class, user.getId());
      if (requested.toInstant().plusSeconds(60).isAfter(now))
        throw new IllegalArgumentException("Повторный код можно запросить через минуту.");
    }
    String code = "%08d".formatted(random.nextInt(100_000_000));
    jdbc.update("DELETE FROM password_changes WHERE user_id=?", user.getId());
    jdbc.update("UPDATE selection_mail_outbox SET expires_at=CURRENT_TIMESTAMP WHERE user_id=? AND kind='PASSWORD_CHANGE' AND sent_at IS NULL", user.getId());
    var expiry = java.time.OffsetDateTime.ofInstant(now.plus(15, ChronoUnit.MINUTES), java.time.ZoneOffset.UTC);
    jdbc.update("INSERT INTO password_changes(user_id,recipient,code_hash,password_hash,requested_at,expires_at) VALUES (?,?,?,?,?,?)",
        user.getId(), user.getEmail(), hash(code), encoder.encode(newPassword),
        java.time.OffsetDateTime.ofInstant(now, java.time.ZoneOffset.UTC), expiry);
    String body = "Здравствуйте, " + user.getDisplayName() + "!\n\n"
        + "Вы запросили смену пароля в «Зелёной лавке».\n"
        + "Код подтверждения: " + code + "\n"
        + "Введите этот код в личном кабинете. Он действует 15 минут с момента запроса.\n"
        + "До подтверждения ваш прежний пароль продолжает работать.\n"
        + "Если вы не запрашивали смену пароля, никому не сообщайте код и не подтверждайте запрос.";
    jdbc.update("INSERT INTO selection_mail_outbox(id,user_id,recipient,body,subject,kind,expires_at) VALUES (?,?,?,?,?,'PASSWORD_CHANGE',?)",
        UUID.randomUUID(), user.getId(), user.getEmail(), body, "Зелёная лавка — подтверждение смены пароля", expiry);
  }

  @Transactional(noRollbackFor = IllegalArgumentException.class)
  public void confirm(String username, String code) {
    var user = users.lockByUsername(username).orElseThrow();
    var requests = jdbc.queryForList("SELECT recipient,code_hash,password_hash,attempts FROM password_changes WHERE user_id=? AND expires_at>CURRENT_TIMESTAMP", user.getId());
    if (requests.isEmpty()) throw new IllegalArgumentException("Запрос не найден или код истёк. Запросите новый код.");
    var row = requests.getFirst();
    if (!row.get("recipient").equals(user.getEmail()))
      throw new IllegalArgumentException("Email изменился. Запросите новый код для смены пароля.");
    if (((Number) row.get("attempts")).intValue() >= 5)
      throw new IllegalArgumentException("Исчерпаны 5 попыток. Запросите новый код.");
    if (code == null || !code.matches("[0-9]{8}") || !MessageDigest.isEqual(
        hash(code).getBytes(StandardCharsets.US_ASCII), ((String) row.get("code_hash")).getBytes(StandardCharsets.US_ASCII))) {
      jdbc.update("UPDATE password_changes SET attempts=attempts+1 WHERE user_id=?", user.getId());
      throw new IllegalArgumentException("Неверный код подтверждения. Доступно не более 5 попыток.");
    }
    user.setPasswordHash((String) row.get("password_hash"));
    jdbc.update("DELETE FROM password_changes WHERE user_id=?", user.getId());
    jdbc.update("UPDATE selection_mail_outbox SET expires_at=CURRENT_TIMESTAMP WHERE user_id=? AND kind='PASSWORD_CHANGE' AND sent_at IS NULL", user.getId());
    String body = "Здравствуйте, " + user.getDisplayName() + "!\nВаш пароль в «Зелёной лавке» успешно изменён.\n"
        + "Для следующего входа используйте новый пароль.\nЕсли это были не вы, обратитесь к администратору магазина.";
    jdbc.update("INSERT INTO selection_mail_outbox(id,user_id,recipient,body,subject,kind) VALUES (?,?,?,?,?,'SECURITY')",
        UUID.randomUUID(), user.getId(), user.getEmail(), body, "Зелёная лавка — пароль изменён");
  }

  private static String hash(String code) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
  }
}
