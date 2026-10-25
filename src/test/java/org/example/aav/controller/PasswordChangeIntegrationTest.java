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

/** Этап 8: добавляем проверки смены пароля, писем и сеансов, чтобы замечать ошибки при изменениях. Учебная последовательность. */
@SpringBootTest
@Transactional
class PasswordChangeIntegrationTest {
  // Контекст Spring: настройки и объекты приложения.
  @Autowired WebApplicationContext context;
  // Сервис регистрации и действий в личном кабинете.
  @Autowired AccountService accounts;
  // Сервис смены пароля с подтверждением по почте.
  @Autowired PasswordChangeService passwords;
  // Создаёт хеши паролей и проверяет пароли.
  @Autowired PasswordEncoder encoder;
  // Помощник Spring для выполнения SQL-запросов.
  @Autowired JdbcTemplate jdbc;
  // Реестр сеансов входа для проверки их завершения.
  @Autowired SessionRegistry sessions;
  // Имитация запросов браузера для проверки контроллеров.
  MockMvc mvc;
  // Уникальный логин тестового пользователя.
  String name;
  // Исходный пароль тестового пользователя.
  static final String OLD = "old-secure-password";
  // Новый пароль для проверки смены пароля.
  static final String NEW = "new-secure-password";

  /**
   * Перед каждым тестом подготавливает запросы и отдельного тестового пользователя.
   */
  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    name = "p" + UUID.randomUUID().toString().replace("-", "");
    // Данные формы, которые нужно проверить или заполнить.
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

  /**
   * После теста возвращает настройку отправки почты в исходное состояние.
   */
  @AfterEach
  void restore() { ReflectionTestUtils.setField(passwords, "mailEnabled", false); }

  /**
   * Возвращает номер тестового пользователя из базы.
   */
  private UUID id() { return accounts.user(name).getId(); }

  /**
   * Извлекает код из тестового письма и проверяет, что пароли не попали в письмо.
   */
  private String code() {
    // Текст письма.
    String body = jdbc.queryForObject("SELECT body FROM selection_mail_outbox WHERE user_id=? AND kind='PASSWORD_CHANGE' ORDER BY created_at DESC LIMIT 1", String.class, id());
    // Поиск кода подтверждения в тексте тестового письма.
    var matcher = Pattern.compile("Код подтверждения: ([0-9]{8})").matcher(body);
    assertThat(matcher.find()).isTrue();
    assertThat(body).doesNotContain(OLD, NEW);
    return matcher.group(1);
  }

  /**
   * Проверяет отдельную страницу смены пароля и обязательный вход.
   */
  @Test
  void passwordSettingsAreSeparateFromProductsAndRequireLogin() throws Exception {
    mvc.perform(get("/account/password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
    mvc.perform(get("/account").with(user(name)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("href=\"/account/password\"")))
        .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"currentPassword\""))));
    mvc.perform(get("/account/password").with(user(name)))
        .andExpect(status().isOk())
        .andExpect(view().name("account-password"))
        .andExpect(content().string(containsString("id=\"currentPassword\"")))
        .andExpect(content().string(containsString("password@example.test")));
    mvc.perform(post("/account/password/request").with(user(name)).with(csrf())
        .param("currentPassword", "wrong").param("newPassword", NEW).param("repeatPassword", NEW))
        .andExpect(redirectedUrl("/account/password"))
        .andExpect(flash().attributeExists("error"));
    mvc.perform(post("/account/password/confirm").with(user(name)).with(csrf()).param("code", "00000000"))
        .andExpect(redirectedUrl("/account/password"))
        .andExpect(flash().attributeExists("error"));
  }

  /**
   * Проверяет смену пароля только после кода и запрет повторного использования кода.
   */
  @Test
  void changesOnlyAfterCodeAndRejectsReuse() {
    passwords.request(name, OLD, NEW, NEW);
    // Код подтверждения смены пароля.
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

  /**
   * Проверяет текущий пароль, совпадение повторного ввода и длину в байтах UTF-8.
   */
  @Test
  void validatesCurrentPasswordRepeatAndUtf8Length() {
    assertThatThrownBy(() -> passwords.request(name, "wrong", NEW, NEW)).hasMessageContaining("Текущий");
    assertThatThrownBy(() -> passwords.request(name, OLD, NEW, "different")).hasMessageContaining("не совпадают");
    assertThatThrownBy(() -> passwords.request(name, OLD, "я".repeat(40), "я".repeat(40))).hasMessageContaining("72 байт");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM password_changes WHERE user_id=?", Integer.class, id())).isZero();
  }

  /**
   * Проверяет срок действия кода и ограничение числа попыток.
   */
  @Test
  void expiresCodesAndLimitsAttempts() {
    passwords.request(name, OLD, NEW, NEW);
    // Правильный код для проверки ограничений.
    String valid = code();
    // Счётчик повторений или номер части письма.
    for (int i = 0; i < 5; i++)
      assertThatThrownBy(() -> passwords.confirm(name, "wrong")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> passwords.confirm(name, valid)).hasMessageContaining("Исчерпаны");
    jdbc.update("UPDATE password_changes SET expires_at=? WHERE user_id=?", java.time.OffsetDateTime.now().minusMinutes(1), id());
    assertThatThrownBy(() -> passwords.confirm(name, valid)).hasMessageContaining("истёк");
    assertThat(encoder.matches(OLD, accounts.user(name).getPasswordHash())).isTrue();
  }

  /**
   * Проверяет отмену старого кода и необходимость нового запроса после смены email.
   */
  @Test
  void resendingInvalidatesPreviousCodeAndEmailChangesRequireNewRequest() {
    passwords.request(name, OLD, NEW, NEW);
    // Код из предыдущего запроса смены пароля.
    String oldCode = code();
    assertThatThrownBy(() -> passwords.request(name, OLD, NEW, NEW)).hasMessageContaining("через минуту");
    jdbc.update("UPDATE password_changes SET requested_at=? WHERE user_id=?", java.time.OffsetDateTime.now().minusMinutes(2), id());
    passwords.request(name, OLD, NEW, NEW);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=? AND kind='PASSWORD_CHANGE' AND expires_at>CURRENT_TIMESTAMP", Integer.class, id())).isOne();
    // Запрос привязан к аккаунту и к email, указанному при запросе кода.
    accounts.user(name).setContacts("Тестов", "+79991234567", "changed@example.test");
    assertThatThrownBy(() -> passwords.confirm(name, oldCode)).hasMessageContaining("Email изменился");
  }

  /**
   * Проверяет подтверждение через защищённую форму и завершение прежних сеансов.
   */
  @Test
  void confirmsThroughCsrfProtectedFormAndExpiresExistingSessions() throws Exception {
    // Результат первого запроса входа.
    var login = mvc.perform(post("/login").with(csrf()).param("username", name).param("password", OLD))
        .andExpect(redirectedUrl("/account")).andReturn();
    // Сеанс входа пользователя.
    var session = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
    // Результат входа во втором сеансе.
    var secondLogin = mvc.perform(post("/login").with(csrf()).param("username", name).param("password", OLD))
        .andExpect(redirectedUrl("/account")).andReturn();
    // Второй сеанс того же пользователя.
    var secondSession = secondLogin.getRequest().getSession(false);
    mvc.perform(post("/account/password/request").session(session)
        .param("currentPassword", OLD).param("newPassword", NEW).param("repeatPassword", NEW))
        .andExpect(status().isForbidden());
    mvc.perform(post("/account/password/request").session(session).with(csrf())
        .param("currentPassword", OLD).param("newPassword", NEW).param("repeatPassword", NEW))
        .andExpect(redirectedUrl("/account/password"));
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

  /**
   * Проверяет запрет подтверждения запроса другим пользователем.
   */
  @Test
  void anotherAccountCannotConfirmTheRequest() {
    passwords.request(name, OLD, NEW, NEW);
    // Код подтверждения владельца аккаунта.
    String token = code();
    // Логин другого тестового пользователя.
    String otherName = "other" + UUID.randomUUID().toString().replace("-", "");
    // Данные формы, которые нужно проверить или заполнить.
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

  /**
   * Проверяет безопасный вывод текста в HTML-письме и подпись магазина.
   */
  @Test
  void mailHtmlEscapesUserContentAndIncludesSignature() {
    // Html-письмо для проверки оформления и безопасного вывода.
    var html = BrandedMail.html("Ваш заказ <script>", "Здравствуйте, <img src=x>!\nИтого: 100 ₽");
    assertThat(html).contains("&lt;script&gt;", "&lt;img src=x&gt;", "#285b3b", "С уважением и заботой")
        .doesNotContain("<script>", "<img src=x>");
  }
}
