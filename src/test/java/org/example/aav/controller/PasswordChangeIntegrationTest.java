package org.example.aav.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import java.util.regex.Pattern;
import org.example.aav.dto.RegistrationForm;
import org.example.aav.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class PasswordChangeIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired AccountService accounts;
  @Autowired PasswordChangeService passwords;
  @Autowired PasswordEncoder encoder;
  @Autowired JdbcTemplate jdbc;
  @Autowired SessionRegistry sessions;
  MockMvc mvc;
  String name;
  static final String OLD = "old-secure-password";
  static final String NEW = "new-secure-password";

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    name = "p" + UUID.randomUUID().toString().replace("-", "");
    var form = new RegistrationForm();
    form.setUsername(name);
    form.setDisplayName("Покупатель <script>");
    form.setLastName("Тестов");
    form.setPhone("+79991234567");
    form.setEmail("password@example.test");
    form.setPassword(OLD);
    accounts.register(form);
    ReflectionTestUtils.setField(passwords, "mailEnabled", true);
  }

  @AfterEach
  void restore() { ReflectionTestUtils.setField(passwords, "mailEnabled", false); }

  private UUID id() { return accounts.user(name).getId(); }

  private String code() {
    String body = jdbc.queryForObject("SELECT body FROM selection_mail_outbox WHERE user_id=? AND kind='PASSWORD_CHANGE' ORDER BY created_at DESC LIMIT 1", String.class, id());
    var matcher = Pattern.compile("Код подтверждения: ([0-9]{8})").matcher(body);
    assertThat(matcher.find()).isTrue();
    assertThat(body).doesNotContain(OLD, NEW);
    return matcher.group(1);
  }

  @Test
  void changesOnlyAfterCodeAndRejectsReuse() {
    passwords.request(name, OLD, NEW, NEW);
    String code = code();
    assertThat(encoder.matches(OLD, accounts.user(name).getPasswordHash())).isTrue();
    assertThat(jdbc.queryForObject("SELECT code_hash FROM password_changes WHERE user_id=?", String.class, id()))
        .hasSize(64).isNotEqualTo(code);
    passwords.confirm(name, code);
    assertThat(encoder.matches(NEW, accounts.user(name).getPasswordHash())).isTrue();
    assertThat(encoder.matches(OLD, accounts.user(name).getPasswordHash())).isFalse();
    assertThatThrownBy(() -> passwords.confirm(name, code)).isInstanceOf(IllegalArgumentException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=? AND kind='SECURITY'", Integer.class, id())).isOne();
  }

  @Test
  void validatesCurrentPasswordRepeatAndUtf8Length() {
    assertThatThrownBy(() -> passwords.request(name, "wrong", NEW, NEW)).hasMessageContaining("Текущий");
    assertThatThrownBy(() -> passwords.request(name, OLD, NEW, "different")).hasMessageContaining("не совпадают");
    assertThatThrownBy(() -> passwords.request(name, OLD, "я".repeat(40), "я".repeat(40))).hasMessageContaining("72 байт");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM password_changes WHERE user_id=?", Integer.class, id())).isZero();
  }

  @Test
  void expiresCodesAndLimitsAttempts() {
    passwords.request(name, OLD, NEW, NEW);
    String valid = code();
    for (int i = 0; i < 5; i++)
      assertThatThrownBy(() -> passwords.confirm(name, "wrong")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> passwords.confirm(name, valid)).hasMessageContaining("Исчерпаны");
    jdbc.update("UPDATE password_changes SET expires_at=? WHERE user_id=?", java.time.OffsetDateTime.now().minusMinutes(1), id());
    assertThatThrownBy(() -> passwords.confirm(name, valid)).hasMessageContaining("истёк");
    assertThat(encoder.matches(OLD, accounts.user(name).getPasswordHash())).isTrue();
  }

  @Test
  void resendingInvalidatesPreviousCodeAndEmailChangesRequireNewRequest() {
    passwords.request(name, OLD, NEW, NEW);
    String oldCode = code();
    assertThatThrownBy(() -> passwords.request(name, OLD, NEW, NEW)).hasMessageContaining("через минуту");
    jdbc.update("UPDATE password_changes SET requested_at=? WHERE user_id=?", java.time.OffsetDateTime.now().minusMinutes(2), id());
    passwords.request(name, OLD, NEW, NEW);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=? AND kind='PASSWORD_CHANGE' AND expires_at>CURRENT_TIMESTAMP", Integer.class, id())).isOne();
    // The row is bound to both the authenticated account and its email at request time.
    accounts.user(name).setContacts("Тестов", "+79991234567", "changed@example.test");
    assertThatThrownBy(() -> passwords.confirm(name, oldCode)).hasMessageContaining("Email изменился");
  }

  @Test
  void confirmsThroughCsrfProtectedFormAndExpiresExistingSessions() throws Exception {
    var login = mvc.perform(post("/login").with(csrf()).param("username", name).param("password", OLD))
        .andExpect(redirectedUrl("/account")).andReturn();
    var session = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
    var secondLogin = mvc.perform(post("/login").with(csrf()).param("username", name).param("password", OLD))
        .andExpect(redirectedUrl("/account")).andReturn();
    var secondSession = secondLogin.getRequest().getSession(false);
    mvc.perform(post("/account/password/request").session(session)
        .param("currentPassword", OLD).param("newPassword", NEW).param("repeatPassword", NEW))
        .andExpect(status().isForbidden());
    mvc.perform(post("/account/password/request").session(session).with(csrf())
        .param("currentPassword", OLD).param("newPassword", NEW).param("repeatPassword", NEW))
        .andExpect(redirectedUrl("/account#password"));
    mvc.perform(post("/account/password/confirm").session(session).with(csrf()).param("code", code()))
        .andExpect(redirectedUrl("/login?passwordChanged"));
    assertThat(session.isInvalid()).isTrue();
    assertThat(sessions.getSessionInformation(secondSession.getId()).isExpired()).isTrue();
    mvc.perform(post("/login").with(csrf()).param("username", name).param("password", OLD))
        .andExpect(redirectedUrl("/login?error"));
    mvc.perform(post("/login").with(csrf()).param("username", name).param("password", NEW))
        .andExpect(redirectedUrl("/account"));
    mvc.perform(get("/login?passwordChanged")).andExpect(content().string(containsString("Пароль изменён")));
  }

  @Test
  void anotherAccountCannotConfirmTheRequest() {
    passwords.request(name, OLD, NEW, NEW);
    String token = code();
    String otherName = "other" + UUID.randomUUID().toString().replace("-", "");
    var form = new RegistrationForm();
    form.setUsername(otherName);
    form.setDisplayName("Другой покупатель");
    form.setLastName("Тестов");
    form.setPhone("+79991234567");
    form.setEmail("other-password@example.test");
    form.setPassword(OLD);
    accounts.register(form);
    assertThatThrownBy(() -> passwords.confirm(otherName, token)).hasMessageContaining("Запрос не найден");
    assertThat(encoder.matches(OLD, accounts.user(name).getPasswordHash())).isTrue();
    assertThat(encoder.matches(OLD, accounts.user(otherName).getPasswordHash())).isTrue();
  }

  @Test
  void mailHtmlEscapesUserContentAndIncludesSignature() {
    var html = BrandedMail.html("Ваш заказ <script>", "Здравствуйте, <img src=x>!\nИтого: 100 ₽");
    assertThat(html).contains("&lt;script&gt;", "&lt;img src=x&gt;", "#285b3b", "С уважением и заботой")
        .doesNotContain("<script>", "<img src=x>");
  }
}
