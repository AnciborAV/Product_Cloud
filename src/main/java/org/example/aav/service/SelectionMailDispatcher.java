package org.example.aav.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SelectionMailDispatcher {
  private static final Logger log = LoggerFactory.getLogger(SelectionMailDispatcher.class);
  private final JdbcTemplate jdbc;
  private final JavaMailSender sender;
  private final String from;

  public SelectionMailDispatcher(JdbcTemplate jdbc, JavaMailSender sender,
      @Value("${app.mail.from}") String from) {
    if (from.isBlank()) throw new IllegalArgumentException("Укажите MAIL_FROM");
    this.jdbc = jdbc;
    this.sender = sender;
    this.from = from;
  }

  @Scheduled(fixedDelayString = "${app.mail.poll-delay:30000}")
  @Transactional
  public void dispatch() {
    var rows = jdbc.queryForList("SELECT id, recipient, body, subject FROM selection_mail_outbox "
        + "WHERE sent_at IS NULL AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP) ORDER BY CASE WHEN kind='PASSWORD_CHANGE' THEN 0 ELSE 1 END, created_at, id LIMIT 20 FOR UPDATE");
    for (var row : rows) {
      var id = (UUID) row.get("id");
      try {
        var message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(new jakarta.mail.internet.InternetAddress(from, "Зелёная лавка", "UTF-8"));
        helper.setTo((String) row.get("recipient"));
        helper.setSubject((String) row.get("subject"));
        String body = (String) row.get("body");
        helper.setText(BrandedMail.plain(body), BrandedMail.html((String) row.get("subject"), body));
        sender.send(message);
        jdbc.update("UPDATE selection_mail_outbox SET sent_at=CURRENT_TIMESTAMP, attempts=attempts+1 WHERE id=?", id);
      } catch (org.springframework.mail.MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
        jdbc.update("UPDATE selection_mail_outbox SET attempts=attempts+1 WHERE id=?", id);
        log.warn("Письмо {} не отправлено ({}), повторим позже", id, ex.getClass().getSimpleName());
        break;
      }
    }
  }
}
