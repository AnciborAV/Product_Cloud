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

@SpringBootTest
class AccountIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired AccountService accounts;
  @Autowired ProductService products;
  @Autowired UserRepository users;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  MockMvc mvc;
  String name;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    name = "u" + UUID.randomUUID().toString().replace("-", "");
    var form = new RegistrationForm();
    form.setUsername(name);
    form.setDisplayName("Покупатель");
    form.setLastName("Тестов");
    form.setPhone("+79991234567");
    form.setEmail("buyer@example.test");
    form.setPassword("long-secure-password");
    accounts.register(form);
  }

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

  @Test
  void protectsMutations() throws Exception {
    String id = products.findAll().getFirst().id().toString();
    mvc.perform(post("/account/products/" + id).with(user(name))).andExpect(status().isForbidden());
    mvc.perform(post("/products").with(user(name)).with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  void savesIdempotentlyAndIsolatesUsers() throws Exception {
    var id = products.findAll().getFirst().id();
    accounts.addSelected(name, id);
    accounts.addSelected(name, id);
    assertThat(accounts.selected(name)).hasSize(1);
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
  void storesReadableNamesAndQueuesUpdatedLists() {
    var product = products.findAll().getFirst();
    accounts.addSelected(name, product.id());
    accounts.addSelected(name, product.id());
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
        String.class, userId)).anyMatch(body -> body.contains("Список пуст"));
  }

  private static String readMail(jakarta.mail.Part part) throws Exception {
    Object content = part.getContent();
    if (content instanceof jakarta.mail.Multipart multipart) {
      var result = new StringBuilder();
      for (int i = 0; i < multipart.getCount(); i++) result.append(readMail(multipart.getBodyPart(i)));
      return result.toString();
    }
    return content.toString();
  }

  @Test
  void retriesFailedMailAndMarksSuccessfulDelivery() {
    accounts.addSelected(name, products.findAll().getFirst().id());
    var userId = accounts.user(name).getId();
    class RecordingSender extends org.springframework.mail.javamail.JavaMailSenderImpl {
      boolean fail = true;
      final java.util.List<jakarta.mail.internet.MimeMessage> messages = new java.util.ArrayList<>();
      @Override public void send(jakarta.mail.internet.MimeMessage message) {
        if (fail) throw new org.springframework.mail.MailSendException("offline");
        messages.add(message);
      }
    }
    var sender = new RecordingSender();
    var dispatcher = new SelectionMailDispatcher(jdbc, sender, "store@example.test");

    dispatcher.dispatch();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=? AND sent_at IS NULL",
        Integer.class, userId)).isEqualTo(1);
    sender.fail = false;
    // Older queued messages may precede this user's message; drain all batches.
    for (int i = 0; i < 10; i++) dispatcher.dispatch();
    assertThat(sender.messages).anySatisfy(message -> {
      try {
        assertThat(java.util.Arrays.stream(message.getAllRecipients()).map(Object::toString))
            .containsExactly("buyer@example.test");
        assertThat(readMail(message)).contains(products.findAll().getFirst().name(), "С уважением", "#285b3b");
      } catch (Exception ex) { throw new AssertionError(ex); }
    });
    assertThat(jdbc.queryForObject("SELECT count(*) FROM selection_mail_outbox WHERE user_id=? AND sent_at IS NOT NULL",
        Integer.class, userId)).isEqualTo(1);
  }

  @Test
  void explicitOrderEmailContainsRecipientItemsAndTotal() throws Exception {
    var product = products.findAll().getFirst();
    accounts.addSelected(name, product.id());
    var controller = context.getBean(AccountController.class);
    org.springframework.test.util.ReflectionTestUtils.setField(controller, "mailEnabled", true);
    try {
      mvc.perform(post("/account/order/email").with(user(name)).with(csrf()))
          .andExpect(redirectedUrl("/account"))
          .andExpect(flash().attribute("message", containsString("buyer@example.test")));
      var bodies = jdbc.queryForList("SELECT body FROM selection_mail_outbox WHERE user_id=?",
          String.class, accounts.user(name).getId());
      assertThat(bodies).hasSize(2).allSatisfy(body ->
          assertThat(body).contains(product.name(), "Итого: " + product.price()));
    } finally {
      org.springframework.test.util.ReflectionTestUtils.setField(controller, "mailEnabled", false);
    }
  }

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

  @Test
  void publishesUpdatesEscapesAndDeletesReviews() throws Exception {
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
    var form = new ReviewForm();
    form.setProductId(id);
    form.setRating(4);
    form.setText("Обновлённый отзыв");
    accounts.saveReview(name, form);
    assertThat(accounts.reviews(name))
        .hasSize(1)
        .first()
        .satisfies(r -> assertThat(r.getRating()).isEqualTo(4));
    var review = accounts.reviews(name).getFirst();
    mvc.perform(
            post("/account/reviews/" + review.getId() + "/remove").with(user(name)).with(csrf()))
        .andExpect(redirectedUrl("/account"));
    assertThat(accounts.reviews(name)).isEmpty();
  }

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

  @Test
  void uploadsRealImageAndRejectsFake() throws Exception {
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

  @Test
  void serializesConcurrentFavoriteAdds() throws Exception {
    var id = products.findAll().getFirst().id();
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var tasks =
          java.util.stream.IntStream.range(0, 8)
              .<java.util.concurrent.Callable<Void>>mapToObj(
                  i ->
                      () -> {
                        accounts.addSelected(name, id);
                        return null;
                      })
              .toList();
      for (var result : executor.invokeAll(tasks)) result.get();
    }
    assertThat(accounts.selected(name)).hasSize(1);
  }

  @Test
  void cannotDeleteAnotherUsersReview() throws Exception {
    var form = new ReviewForm();
    form.setProductId(products.findAll().getFirst().id());
    form.setText("Мой отзыв");
    accounts.saveReview(name, form);
    var review = accounts.reviews(name).getFirst();
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
