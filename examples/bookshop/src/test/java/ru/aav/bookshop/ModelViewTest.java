package ru.aav.bookshop;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ru.aav.bookshop.model.*;
import ru.aav.bookshop.repository.LibraryRepository;
import ru.aav.bookshop.security.Passwords;
import ru.aav.bookshop.service.*;
import ru.aav.bookshop.view.ShopView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Быстрые тесты без базы: арифметика, проверки, пароли и настоящие HTML-шаблоны. */
class ModelViewTest {
    /**
     * Проверяет точность сумм и неизменяемость списка строк заказа. {@code @Test} отмечает
     * независимый сценарий JUnit.
     */
    @Test
    void moneyHasNoFloatingPointError() {
        var order =
                new Order(
                        UUID.randomUUID(),
                        Instant.now(),
                        List.of(
                                new Order.Line(1, "Книга", new BigDecimal("0.10"), 3),
                                new Order.Line(2, "Другая", new BigDecimal("0.20"), 1)));
        assertEquals(new BigDecimal("0.50"), order.total());
        assertThrows(UnsupportedOperationException.class, () -> order.lines().clear());
    }

    /**
     * Проверяет отказ некорректного заказа до обращения к базе. {@code @Test} отмечает независимый
     * сценарий JUnit.
     */
    @Test
    void invalidOrdersAreRejectedBeforeDatabaseAccess() {
        var service = new ShopService(null, null);
        assertThrows(IllegalArgumentException.class, () -> service.checkout(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> service.checkout(Map.of(1L, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.checkout(Map.of(1L, 11)));
    }

    /**
     * Проверяет отказ недопустимого PDF до обращения к базе. {@code @Test} отмечает независимый
     * сценарий JUnit.
     */
    @Test
    void invalidFilesAreRejectedBeforeDatabaseAccess() {
        var service = new LibraryService(null);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(1, "A", "B", "x.pdf", "hello".getBytes()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.upload(
                                1, "A", "B", "x.pdf", new byte[LibraryService.MAX_PDF_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> service.createShelf(" "));
    }

    /**
     * Проверяет случайность соли и проверку правильного и неправильного пароля. {@code @Test}
     * отмечает независимый сценарий JUnit.
     */
    @Test
    void passwordsAreSaltedAndVerified() {
        String first = Passwords.hash("test-password"), second = Passwords.hash("test-password");
        assertNotEquals(first, second);
        assertFalse(first.contains("test-password"));
        assertTrue(Passwords.verify("test-password", first));
        assertFalse(Passwords.verify("wrong", first));
    }

    /**
     * Проверяет рендеринг всех шаблонов и экранирование пользовательского текста. {@code @Test}
     * отмечает независимый сценарий JUnit.
     */
    @Test
    void thymeleafRendersEveryPageAndEscapesUntrustedText() {
        var view = new ShopView();
        var book =
                new Book(
                        1,
                        "<script>alert(1)</script>",
                        "Автор",
                        "Проза",
                        new BigDecimal("640.00"),
                        "olive",
                        "Описание");
        String catalog = view.catalog(List.of(book));
        assertTrue(catalog.contains("&lt;script&gt;"));
        assertFalse(catalog.contains("<script>alert(1)"));
        assertTrue(catalog.contains("data-id=\"1\""));
        assertFalse(catalog.contains("th:each"));
        assertTrue(view.login(true).contains("data-action=\"/setup\""));
        assertTrue(view.login(false).contains("data-action=\"/login\""));
        var id = UUID.randomUUID();
        String library =
                view.library(
                        List.of(new LibraryRepository.Shelf(1, "Ницше")),
                        List.of(
                                new LibraryDocument(
                                        id, 1, "Ницше", "<b>Книга</b>", "Автор", 1048576)));
        assertTrue(library.contains("/files/" + id));
        assertTrue(library.contains("&lt;b&gt;Книга&lt;/b&gt;"));
        assertTrue(view.library(List.of(), List.of()).contains("Первая книга"));
        assertTrue(
                view.receipt(
                                new Order(
                                        id,
                                        Instant.now(),
                                        List.of(
                                                new Order.Line(
                                                        1, "Книга", new BigDecimal("640"), 2))))
                        .contains("Книга × 2"));
        assertTrue(view.error("<img>").contains("&lt;img&gt;"));
    }
}
