package ru.aav;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ru.aav.account.CustomerAccount;
import ru.aav.cart.CartView;
import ru.aav.order.OrderView;

import java.math.BigDecimal;
import java.util.ArrayList;

/** Проверяет, что внешние изменяемые объекты не могут незаметно изменить сохранённые снимки. */
class ViewIsolationTests {
    /** DTO корзины копирует входную коллекцию и запрещает изменение через свой accessor. */
    @Test
    void cartViewCopiesInputLines() {
        var lines = new ArrayList<CartView.Line>();
        lines.add(new CartView.Line(1, "Хлеб", 1, BigDecimal.ONE, BigDecimal.ONE));
        var view = new CartView(lines, 1, BigDecimal.ONE);
        lines.clear();
        assertEquals(1, view.lines().size());
        assertThrows(UnsupportedOperationException.class, () -> view.lines().clear());
    }

    /** DTO заказа не разделяет изменяемую коллекцию с вызывающим кодом. */
    @Test
    void orderViewCopiesInputLines() {
        var lines = new ArrayList<OrderView.Line>();
        lines.add(new OrderView.Line(1, "Хлеб", 1, BigDecimal.ONE, BigDecimal.ONE));
        var view = new OrderView(1, "дата", "Имя", "Телефон", "Адрес", "", lines, BigDecimal.ONE);
        lines.clear();
        assertEquals(1, view.lines().size());
        assertThrows(UnsupportedOperationException.class, () -> view.lines().clear());
    }

    /** Конструктор, setter и getter фотографии защищают массив от изменений извне. */
    @Test
    void accountCopiesAvatarOnInputAndOutput() {
        byte[] original = {1, 2, 3};
        var account = new CustomerAccount("test@example.com", "hash", "Имя", original);
        original[0] = 9;
        assertEquals(1, account.getAvatar()[0]);
        byte[] returned = account.getAvatar();
        returned[0] = 8;
        assertEquals(1, account.getAvatar()[0]);
        account.setAvatar(original);
        original[0] = 7;
        assertEquals(9, account.getAvatar()[0]);
        account.setAvatar(null);
        assertFalse(account.hasAvatar());
        assertNull(account.getAvatar());
    }
}
