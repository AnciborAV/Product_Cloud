package org.example.aav.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.example.aav.dto.RegistrationForm;
import org.example.aav.dto.ReviewForm;
import org.example.aav.repository.*;
import org.example.aav.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Этап 8: добавляем проверки совместной работы регистрации, личного кабинета и базы, чтобы замечать ошибки при изменениях. Учебная последовательность. */
@SpringBootTest
class AccountIntegrationTest {
  // Контекст Spring: настройки и объекты приложения.
  @Autowired WebApplicationContext context;
  // Сервис регистрации и действий в личном кабинете.
  @Autowired AccountService accounts;
  // Сервис поиска и добавления товаров.
  @Autowired ProductService products;
  // Доступ к учётным записям в базе данных.
  @Autowired UserRepository users;
  // Помощник Spring для выполнения SQL-запросов.
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  // Имитация запросов браузера для проверки контроллеров.
  MockMvc mvc;
  // Уникальный логин тестового пользователя.
  String name;

  /**
   * Перед каждым тестом подготавливает запросы и отдельного тестового пользователя.
   */
  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    name = "u" + UUID.randomUUID().toString().replace("-", "");
    // Данные формы, которые нужно проверить или заполнить.
    var form = new RegistrationForm();
    form.setUsername(name);
    form.setDisplayName("Покупатель");
    form.setLastName("Тестов");
    form.setPhone("+79991234567");
    form.setEmail("buyer@example.test");
    form.setPassword("long-secure-password");
    accounts.register(form);
  }

  /**
   * Проверяет открытие общедоступных страниц и защиту личного кабинета.
   */
  @Test
  void rendersPublicAndPrivatePages() throws Exception {
    mvc.perform(get("/login")).andExpect(status().isOk());
    mvc.perform(get("/register")).andExpect(status().isOk());
    mvc.perform(get("/account")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/account").with(user(name)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Покупатель")));
    mvc.perform(get("/reviews")).andExpect(status().isOk());
  }

  /**
   * Проверяет вход пользователя и хранение хеша вместо открытого пароля.
   */
  @Test
  void authenticatesAndHashesPassword() throws Exception {
    assertThat(users.findByUsername(name).orElseThrow().getPasswordHash()).startsWith("$2");
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", name)
                .param("password", "long-secure-password"))
        .andExpect(redirectedUrl("/account"));
    mvc.perform(post("/login").with(csrf()).param("username", name).param("password", "wrong"))
        .andExpect(redirectedUrl("/login?error"));
  }

  /**
   * Проверяет защиту запросов, изменяющих данные.
   */
  @Test
  void protectsMutations() throws Exception {
    // Уникальный номер записи.
    String id = products.findAll().getFirst().id().toString();
    mvc.perform(post("/account/products/" + id).with(user(name))).andExpect(status().isForbidden());
    mvc.perform(post("/products").with(user(name)).with(csrf())).andExpect(status().isForbidden());
  }

  /**
   * Проверяет отсутствие повторных добавлений и разделение данных пользователей.
   */
  @Test
  void savesIdempotentlyAndIsolatesUsers() throws Exception {
    // Уникальный номер записи.
    var id = products.findAll().getFirst().id();
    accounts.addSelected(name, id);
    accounts.addSelected(name, id);
    assertThat(accounts.selected(name)).hasSize(1);
    // Форма другого пользователя для проверки разделения доступа.
    var other = new RegistrationForm();
    other.setUsername(name + "x");
    other.setDisplayName("Другой");
    other.setLastName("Тестов");
    other.setPhone("+79991234567");
    other.setEmail("other@example.test");
    other.setPassword("another-long-password");
    accounts.register(other);
    accounts.removeSelected(other.getUsername(), id);
    assertThat(accounts.selected(name)).hasSize(1);
    accounts.removeSelected(name, id);
    assertThat(accounts.selected(name)).isEmpty();
  }

  @Test
  void removesSelectedProductsTogetherAndQueuesOneUpdate() throws Exception {
    var items = products.findAll().subList(0, 3);
    for (var item : items) accounts.addSelected(name, item.id());
    var userId = accounts.user(name).getId();
    mvc.perform(get("/account").with(user(name)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("select-all-products")))
        .andExpect(content().string(containsString("Удалить выбранные")));
    mvc.perform(post("/account/products/remove-selected").with(user(name))
        .param("productIds", items.getFirst().id().toString()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/account/products/remove-selected").with(user(name)).with(csrf())
        .param("productIds", items.get(0).id().toString(), items.get(1).id().toString(),
            items.get(0).id().toString(), UUID.randomUUID().toString()))
        .andExpect(redirectedUrl("/account"))
        .andExpect(flash().attribute("message", "Удалено товаров: 2"));
    assertThat(accounts.selected(name)).extracting(p -> p.id()).containsExactly(items.get(2).id());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=?",
        Integer.class, userId)).isEqualTo(4);
    mvc.perform(post("/account/products/remove-selected").with(user(name)).with(csrf()))
        .andExpect(redirectedUrl("/account"));
    mvc.perform(post("/account/products/remove-selected").with(user(name)).with(csrf())
        .param("productIds", items.get(2).id().toString()))
        .andExpect(redirectedUrl("/account"));
    assertThat(accounts.selected(name)).isEmpty();
  }

  @Test
  void bulkRemovalDoesNotAffectAnotherUsersSelection() {
    var id = products.findAll().getFirst().id();
    accounts.addSelected(name, id);
    var other = new RegistrationForm();
    other.setUsername(name + "other");
    other.setDisplayName("Другой");
    other.setLastName("Тестов");
    other.setPhone("+79991234567");
    other.setEmail("other@example.test");
    other.setPassword("another-long-password");
    accounts.register(other);
    accounts.addSelected(other.getUsername(), id);
    assertThat(accounts.removeSelectedProducts(other.getUsername(), java.util.List.of(id))).isEqualTo(1);
    assertThat(accounts.selected(other.getUsername())).isEmpty();
    assertThat(accounts.selected(name)).hasSize(1);
  }

  /**
   * Проверяет сохранение названий товаров и постановку обновлённых списков в очередь.
   */
  @Test
  void storesReadableNamesAndQueuesUpdatedLists() {
    // Товар, с которым выполняется действие.
    var product = products.findAll().getFirst();
    accounts.addSelected(name, product.id());
    accounts.addSelected(name, product.id());
    // Уникальный номер пользователя.
    var userId = accounts.user(name).getId();
    assertThat(jdbc.queryForObject("SELECT product_name FROM selected_products WHERE user_id=?",
        String.class, userId)).isEqualTo(product.name());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=?",
        Integer.class, userId)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT body FROM selection_mail_outbox WHERE user_id=?",
        String.class, userId)).contains(product.name());
    accounts.removeSelected(name, product.id());
    accounts.removeSelected(name, product.id());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=?",
        Integer.class, userId)).isEqualTo(2);
    assertThat(jdbc.queryForList("SELECT body FROM selection_mail_outbox WHERE user_id=?",
        // body — текст очередного письма.
        String.class, userId)).anyMatch(body -> body.contains("Список пуст"));
  }

  /**
   * Читает текст тестового письма, включая вложенные части.
   * @param part часть письма: текст или вложенное содержимое
   */
  private static String readMail(jakarta.mail.Part part) throws Exception {
    // Собираемое содержимое письма.
    Object content = part.getContent();
    // multipart — объект после проверки и уточнения его типа через instanceof.
    if (content instanceof jakarta.mail.Multipart multipart) {
      // Результат текущей операции.
      var result = new StringBuilder();
      // Счётчик повторений или номер части письма.
      for (int i = 0; i < multipart.getCount(); i++) result.append(readMail(multipart.getBodyPart(i)));
      return result.toString();
    }
    return content.toString();
  }

  /**
   * Проверяет повтор после ошибки отправки и отметку успешной доставки.
   */
  @Test
  void retriesFailedMailAndMarksSuccessfulDelivery() {
    accounts.addSelected(name, products.findAll().getFirst().id());
    // Уникальный номер пользователя.
    var userId = accounts.user(name).getId();
    /** Этап 8: создаём тестовый отправитель писем, чтобы проверять сбои без настоящей отправки. Учебная последовательность. */
    class RecordingSender extends org.springframework.mail.javamail.JavaMailSenderImpl {
      // Нужно ли имитировать ошибку отправки письма.
      boolean fail = true;
      // Письма, которые тестовый отправитель принял для отправки.
      final java.util.List<jakarta.mail.internet.MimeMessage> messages = new java.util.ArrayList<>();
      /**
       * Имитирует отправку письма: создаёт ошибку или запоминает письмо для проверки.
       * @param message письмо для отправки через почтовый сервер
       */
      @Override public void send(jakarta.mail.internet.MimeMessage message) {
        if (fail) throw new org.springframework.mail.MailSendException("offline");
        messages.add(message);
      }
    }
    // Средство отправки писем через почтовый сервер.
    var sender = new RecordingSender();
    // Обработчик очереди писем.
    var dispatcher = new SelectionMailDispatcher(jdbc, sender, "store@example.test");

    dispatcher.dispatch();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=? AND sent_at IS NULL",
        Integer.class, userId)).isEqualTo(1);
    sender.fail = false;
    // Раньше могли накопиться другие письма: обрабатываем очередь несколькими порциями.
    // Счётчик повторений или номер части письма.
    for (int i = 0; i < 10; i++) dispatcher.dispatch();
    // message — очередное тестовое письмо.
    assertThat(sender.messages).anySatisfy(message -> {
      try {
        assertThat(java.util.Arrays.stream(message.getAllRecipients()).map(Object::toString))
            .containsExactly("buyer@example.test");
        assertThat(readMail(message)).contains(products.findAll().getFirst().name(), "С уважением", "#285b3b");
      // ex — ошибка, которую перехватываем, чтобы обработать её здесь.
      } catch (Exception ex) { throw new AssertionError(ex); }
    });
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=? AND sent_at IS NOT NULL",
        Integer.class, userId)).isEqualTo(1);
  }

  /**
   * Проверяет адрес получателя, товары и итоговую сумму письма с заказом.
   */
  @Test
  void explicitOrderEmailContainsRecipientItemsAndTotal() throws Exception {
    // Товар, с которым выполняется действие.
    var product = products.findAll().getFirst();
    accounts.addSelected(name, product.id());
    // Контроллер из контекста Spring для проверки его работы.
    var controller = context.getBean(AccountController.class);
    org.springframework.test.util.ReflectionTestUtils.setField(controller, "mailEnabled", true);
    try {
      mvc.perform(post("/account/order/email").with(user(name)).with(csrf()))
          .andExpect(redirectedUrl("/account"))
          .andExpect(flash().attribute("message", containsString("buyer@example.test")));
      // Тексты писем из очереди отправки.
      var bodies = jdbc.queryForList("SELECT body FROM selection_mail_outbox WHERE user_id=?",
          String.class, accounts.user(name).getId());
      // body — текст очередного письма.
      assertThat(bodies).hasSize(2).allSatisfy(body ->
          assertThat(body).contains(product.name(), "Итого: " + product.price()));
    } finally {
      org.springframework.test.util.ReflectionTestUtils.setField(controller, "mailEnabled", false);
    }
  }

  /**
   * Проверяет сообщение об отключённой почте без добавления заказа в очередь.
   */
  @Test
  void disabledMailExplainsProblemWithoutQueuingOrder() throws Exception {
    mvc.perform(post("/account/order/email").with(user(name)))
        .andExpect(status().isForbidden());
    mvc.perform(post("/account/order/email").with(user(name)).with(csrf()))
        .andExpect(redirectedUrl("/account"))
        .andExpect(flash().attribute("error", containsString("SMTP")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=?",
        Integer.class, accounts.user(name).getId())).isZero();
    assertThatThrownBy(() -> accounts.emailOrder(name)).isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * Проверяет обновление контактов и письмо с текущим списком товаров.
   */
  @Test
  void updatesContactDetailsAndQueuesCurrentList() throws Exception {
    mvc.perform(post("/account/contacts").with(user(name)).with(csrf())
        .param("displayName", "Анна").param("lastName", "Иванова")
        .param("phone", "+79991234567").param("email", "anna@example.test"))
        .andExpect(redirectedUrl("/account"));
    assertThat(accounts.user(name).getLastName()).isEqualTo("Иванова");
    assertThat(accounts.user(name).getEmail()).isEqualTo("anna@example.test");
    assertThat(jdbc.queryForObject("SELECT recipient FROM selection_mail_outbox WHERE user_id=?",
        String.class, accounts.user(name).getId())).isEqualTo("anna@example.test");
  }

  /**
   * Проверяет создание, изменение, безопасный показ и удаление отзыва.
   */
  @Test
  void publishesUpdatesEscapesAndDeletesReviews() throws Exception {
    // Уникальный номер записи.
    var id = products.findAll().getFirst().id();
    mvc.perform(
            post("/account/reviews")
                .with(user(name))
                .with(csrf())
                .param("productId", id.toString())
                .param("rating", "5")
                .param("text", "<script>test</script>"))
        .andExpect(redirectedUrl("/account"));
    mvc.perform(get("/reviews"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("&lt;script&gt;test&lt;/script&gt;")));
    // Данные формы, которые нужно проверить или заполнить.
    var form = new ReviewForm();
    form.setProductId(id);
    form.setRating(4);
    form.setText("Обновлённый отзыв");
    accounts.saveReview(name, form);
    assertThat(accounts.reviews(name))
        .hasSize(1)
        .first()
        // r — очередной отзыв для проверки его номера.
        .satisfies(r -> assertThat(r.getRating()).isEqualTo(4));
    // Значение для текущей операции.
    var review = accounts.reviews(name).getFirst();
    mvc.perform(
            post("/account/reviews/" + review.getId() + "/remove").with(user(name)).with(csrf()))
        .andExpect(redirectedUrl("/account"));
    assertThat(accounts.reviews(name)).isEmpty();
  }

  /**
   * Проверяет проверку допустимой оценки и текста отзыва.
   */
  @Test
  void validatesReviews() throws Exception {
    mvc.perform(
            post("/account/reviews")
                .with(user(name))
                .with(csrf())
                .param("rating", "6")
                .param("text", " "))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("reviewForm", "rating", "text", "productId"));
  }

  /**
   * Проверяет загрузку настоящего изображения и отказ для поддельного файла.
   */
  @Test
  void uploadsRealImageAndRejectsFake() throws Exception {
    // Поток, в котором собираются байты изображения.
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", output);
    mvc.perform(
            multipart("/account/avatar")
                .file(new MockMultipartFile("file", "photo.png", "image/png", output.toByteArray()))
                .with(user(name))
                .with(csrf()))
        .andExpect(redirectedUrl("/account"));
    mvc.perform(get("/account/avatar").with(user(name)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"));
    mvc.perform(
            multipart("/account/avatar")
                .file(new MockMultipartFile("file", "bad.png", "image/png", "fake".getBytes()))
                .with(user(name))
                .with(csrf()))
        .andExpect(flash().attributeExists("error"));
  }

  /**
   * Проверяет отказ при незаполненных полях регистрации и занятом логине.
   */
  @Test
  void rejectsMissingRegistrationFieldsAndDuplicateLogin() throws Exception {
    mvc.perform(post("/register").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "registrationForm", "username", "displayName", "password"));
    mvc.perform(
            post("/register")
                .with(csrf())
                .param("username", name.toUpperCase(java.util.Locale.ROOT))
                .param("displayName", "Покупатель")
                .param("lastName", "Тестов")
                .param("phone", "+79991234567")
                .param("email", "buyer@example.test")
                .param("password", "long-secure-password"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("registrationForm", "username"));
  }

  /**
   * Проверяет согласованность списка при одновременном добавлении товара.
   */
  @Test
  void serializesConcurrentFavoriteAdds() throws Exception {
    // Уникальный номер записи.
    var id = products.findAll().getFirst().id();
    // Средство параллельного запуска действий в тесте.
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      // Значение для текущей операции.
      var tasks =
          java.util.stream.IntStream.range(0, 8)
              .<java.util.concurrent.Callable<Void>>mapToObj(
                  // i — очередной элемент для обработки.
                  i ->
                      () -> {
                        accounts.addSelected(name, id);
                        return null;
                      })
              .toList();
      // Результат текущей операции.
      for (var result : executor.invokeAll(tasks)) result.get();
    }
    assertThat(accounts.selected(name)).hasSize(1);
  }

  /**
   * Проверяет запрет удаления и редактирования чужого отзыва.
   */
  @Test
  void cannotDeleteAnotherUsersReview() throws Exception {
    // Данные формы, которые нужно проверить или заполнить.
    var form = new ReviewForm();
    form.setProductId(products.findAll().getFirst().id());
    form.setText("Мой отзыв");
    accounts.saveReview(name, form);
    // Значение для текущей операции.
    var review = accounts.reviews(name).getFirst();
    // Форма другого пользователя для проверки разделения доступа.
    var other = new RegistrationForm();
    other.setUsername(name + "x");
    other.setDisplayName("Другой");
    other.setLastName("Тестов");
    other.setPhone("+79991234567");
    other.setEmail("other@example.test");
    other.setPassword("another-long-password");
    accounts.register(other);
    mvc.perform(
            post("/account/reviews/" + review.getId() + "/remove")
                .with(user(other.getUsername()))
                .with(csrf()))
        .andExpect(redirectedUrl("/account"));
    assertThat(accounts.reviews(name)).hasSize(1);
    mvc.perform(get("/account/reviews/" + review.getId() + "/edit").with(user(other.getUsername())))
        .andExpect(status().isNotFound());
    mvc.perform(get("/account/reviews/" + review.getId() + "/edit").with(user(name)))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("reviewForm"));
    accounts.removeReview(name, review.getId());
  }
}
