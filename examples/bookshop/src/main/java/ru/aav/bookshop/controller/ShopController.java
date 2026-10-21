package ru.aav.bookshop.controller;

import com.sun.net.httpserver.*;

import ru.aav.bookshop.service.ShopService;
import ru.aav.bookshop.view.ShopView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Контроллер маршрутизирует HTTP-запрос, вызывает сервис и выбирает представление. */
public final class ShopController implements HttpHandler {
    /** Сервис бизнес-операций, отделённый от HTTP. */
    private final ShopService service;

    /** Компонент формирования HTML-представлений. */
    private final ShopView view;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public ShopController(ShopService service, ShopView view) {
        this.service = service;
        this.view = view;
    }

    /**
     * Выбирает маршрут каталога, оформления или чека и преобразует ошибки ввода в HTTP-ответ.
     * {@code @Override} позволяет компилятору проверить, что метод переопределяет существующий
     * контракт.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Путь URL, по которому выбирается обработчик; это не путь к файлу на диске.
            String path = exchange.getRequestURI().getPath();
            // HTTP-метод запроса: определяет допустимое действие маршрута.
            String method = exchange.getRequestMethod();
            if ("GET".equals(method)) {
                switch (path) {
                    case "/" -> send(exchange, 200, "text/html", view.catalog(service.catalog()));
                    case "/style.css" ->
                            send(exchange, 200, "text/css", ShopView.resource("style.css"));
                    case "/shop.js" ->
                            send(exchange, 200, "text/javascript", ShopView.resource("shop.js"));
                    default -> {
                        if (path.matches("/orders/[0-9a-fA-F-]{36}")) {
                            // Созданный или найденный заказ; дальнейший ответ содержит его
                            // идентификатор или представление.
                            var order = service.findOrder(UUID.fromString(path.substring(8)));
                            send(
                                    exchange,
                                    order.isPresent() ? 200 : 404,
                                    "text/html",
                                    order.isPresent()
                                            ? view.receipt(order.get())
                                            : view.error("Заказ не найден."));
                        } else send(exchange, 404, "text/html", view.error("Страница не найдена."));
                    }
                }
            } else if ("POST".equals(method) && "/orders".equals(path)) {
                // Нестандартный заголовок требует CORS preflight при обращении с чужого сайта.
                // CORS не разрешён: сторонняя страница не может оформить заказ в локальном
                // магазине.
                if (!"bookshop".equals(exchange.getRequestHeaders().getFirst("X-Bookshop"))) {
                    send(exchange, 403, "text/plain", "Оформляйте заказ через страницу магазина.");
                    return;
                }
                // Тело запроса с проверенным ограничением размера.
                byte[] body = exchange.getRequestBody().readNBytes(4097);
                if (body.length > 4096) {
                    send(exchange, 413, "text/plain", "Слишком большой заказ.");
                    return;
                }
                // Созданный или найденный заказ; дальнейший ответ содержит его идентификатор или
                // представление.
                var order = service.checkout(parseItems(new String(body, StandardCharsets.UTF_8)));
                exchange.getResponseHeaders().set("Location", "/orders/" + order.id());
                send(exchange, 201, "text/plain", "/orders/" + order.id());
            } else {
                exchange.getResponseHeaders().set("Allow", "/orders".equals(path) ? "POST" : "GET");
                send(exchange, 405, "text/plain", "Метод не поддерживается.");
            }
        } catch (IllegalArgumentException exception) {
            send(
                    exchange,
                    400,
                    "text/plain",
                    "Проверьте заказ: допустимы книги из каталога, количество от 1 до 10.");
        } catch (Exception exception) {
            exception.printStackTrace();
            send(
                    exchange,
                    500,
                    "text/plain",
                    "Не удалось выполнить запрос. Проверьте журнал сервера; корзина сохранена.");
        } finally {
            exchange.close();
        }
    }

    /** Компактный протокол тела запроса: 1=2&3=1 означает две книги №1 и одну №3. */
    private Map<Long, Integer> parseItems(String body) {
        // Позиции, которые будут проверены и преобразованы в состав заказа.
        Map<Long, Integer> items = new LinkedHashMap<>();
        if (body.isBlank()) return items;
        for (String pair : body.split("&", -1)) {
            if (!pair.matches("[0-9]{1,9}=[0-9]{1,2}")) throw new IllegalArgumentException();
            // Части компактного строкового формата; используются для восстановления значений.
            String[] parts = pair.split("=");
            if (items.put(Long.parseLong(parts[0]), Integer.parseInt(parts[1])) != null)
                throw new IllegalArgumentException("Повторная позиция.");
        }
        return items;
    }

    /**
     * Передаёт отправку ответа общему Http, чтобы заголовки безопасности и кодировка задавались в
     * одном месте.
     */
    private void send(HttpExchange exchange, int status, String type, String body)
            throws IOException {
        Http.send(exchange, status, type, body);
    }
}
