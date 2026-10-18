package ru.aav.bookshop.controller;

import com.sun.net.httpserver.*;
import ru.aav.bookshop.service.ShopService;
import ru.aav.bookshop.view.ShopView;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Контроллер маршрутизирует HTTP-запрос, вызывает сервис и выбирает представление. */
public final class ShopController implements HttpHandler {
    private final ShopService service;
    private final ShopView view;
    public ShopController(ShopService service, ShopView view) { this.service = service; this.view = view; }

    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method)) {
                switch (path) {
                    case "/" -> send(exchange, 200, "text/html", view.catalog(service.catalog()));
                    case "/style.css" -> send(exchange, 200, "text/css", ShopView.resource("style.css"));
                    case "/shop.js" -> send(exchange, 200, "text/javascript", ShopView.resource("shop.js"));
                    default -> {
                        if (path.matches("/orders/[0-9a-fA-F-]{36}")) {
                            var order = service.findOrder(UUID.fromString(path.substring(8)));
                            send(exchange, order.isPresent() ? 200 : 404, "text/html",
                                order.isPresent() ? view.receipt(order.get()) : view.error("Заказ не найден."));
                        } else send(exchange, 404, "text/html", view.error("Страница не найдена."));
                    }
                }
            } else if ("POST".equals(method) && "/orders".equals(path)) {
                // Нестандартный заголовок требует CORS preflight при обращении с чужого сайта.
                // CORS не разрешён: сторонняя страница не может оформить заказ в локальном магазине.
                if (!"bookshop".equals(exchange.getRequestHeaders().getFirst("X-Bookshop"))) {
                    send(exchange, 403, "text/plain", "Оформляйте заказ через страницу магазина.");
                    return;
                }
                byte[] body = exchange.getRequestBody().readNBytes(4097);
                if (body.length > 4096) {
                    send(exchange, 413, "text/plain", "Слишком большой заказ."); return;
                }
                var order = service.checkout(parseItems(new String(body, StandardCharsets.UTF_8)));
                exchange.getResponseHeaders().set("Location", "/orders/" + order.id());
                send(exchange, 201, "text/plain", "/orders/" + order.id());
            } else {
                exchange.getResponseHeaders().set("Allow", "/orders".equals(path) ? "POST" : "GET");
                send(exchange, 405, "text/plain", "Метод не поддерживается.");
            }
        } catch (IllegalArgumentException exception) {
            send(exchange, 400, "text/plain", "Проверьте заказ: допустимы книги из каталога, количество от 1 до 10.");
        } catch (Exception exception) {
            exception.printStackTrace();
            send(exchange, 500, "text/plain", "Не удалось выполнить запрос. Проверьте журнал сервера; корзина сохранена.");
        } finally { exchange.close(); }
    }

    /** Компактный протокол тела запроса: 1=2&3=1 означает две книги №1 и одну №3. */
    private Map<Long, Integer> parseItems(String body) {
        Map<Long, Integer> items = new LinkedHashMap<>();
        if (body.isBlank()) return items;
        for (String pair : body.split("&", -1)) {
            if (!pair.matches("[0-9]{1,9}=[0-9]{1,2}")) throw new IllegalArgumentException();
            String[] parts = pair.split("=");
            if (items.put(Long.parseLong(parts[0]), Integer.parseInt(parts[1])) != null)
                throw new IllegalArgumentException("Повторная позиция.");
        }
        return items;
    }
    private void send(HttpExchange exchange, int status, String type, String body) throws IOException {
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", type + "; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
