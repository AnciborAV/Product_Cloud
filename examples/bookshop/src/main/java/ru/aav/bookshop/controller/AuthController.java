package ru.aav.bookshop.controller;

import com.sun.net.httpserver.*;
import ru.aav.bookshop.security.AuthService;
import ru.aav.bookshop.view.ShopView;
import java.io.IOException;
import java.util.Set;

/** Единая проверка доступа: даже прямая ссылка на PDF требует действующей сессии. */
public final class AuthController implements HttpHandler {
    private final AuthService auth;
    private final HttpHandler shop, library;
    private final ShopView view;
    private static final Set<String> ASSETS = Set.of("/style.css", "/shop.js", "/library.js", "/auth.js");
    public AuthController(AuthService auth, HttpHandler shop, HttpHandler library, ShopView view) {
        this.auth = auth; this.shop = shop; this.library = library; this.view = view;
    }
    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            // Разрешены только локальные имена хоста: защита от DNS rebinding к локальному сервису.
            String host = exchange.getRequestHeaders().getFirst("Host");
            if (host == null || !host.matches("(localhost|127\\.0\\.0\\.1)(:[0-9]+)?")) {
                Http.send(exchange, 403, "text/plain", "Используйте localhost."); return;
            }
            if (method.equals("POST") && !"bookshop".equals(exchange.getRequestHeaders().getFirst("X-Bookshop"))) {
                Http.send(exchange, 403, "text/plain", "Запрос должен быть отправлен из приложения."); return;
            }
            if (method.equals("GET") && ASSETS.contains(path)) {
                Http.send(exchange, 200, path.endsWith(".css") ? "text/css" : "text/javascript", ShopView.resource(path.substring(1))); return;
            }
            if (method.equals("GET") && (path.equals("/login") || path.equals("/setup"))) {
                boolean configured = auth.configured();
                if (auth.authenticated(token(exchange))) { Http.redirect(exchange, "/library"); return; }
                Http.send(exchange, 200, "text/html", view.login(!configured)); return;
            }
            if (method.equals("POST") && (path.equals("/login") || path.equals("/setup"))) {
                var form = Http.form(exchange);
                String email = form.getOrDefault("email", ""), password = form.getOrDefault("password", "");
                if (path.equals("/setup")) auth.setup(email, password);
                var session = auth.login(email, password);
                if (session.isEmpty()) { Http.send(exchange, 401, "text/plain", "Неверный email или пароль."); return; }
                exchange.getResponseHeaders().set("Set-Cookie", "shelf_session=" + session.get() + "; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800");
                Http.send(exchange, 200, "text/plain", "/library"); return;
            }
            if (!auth.authenticated(token(exchange))) {
                if (method.equals("GET")) Http.redirect(exchange, "/login");
                else Http.send(exchange, 401, "text/plain", "Сеанс завершён. Войдите снова.");
                return;
            }
            if (method.equals("POST") && path.equals("/logout")) {
                auth.logout(token(exchange));
                exchange.getResponseHeaders().set("Set-Cookie", "shelf_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
                Http.send(exchange, 200, "text/plain", "/login"); return;
            }
            if (path.equals("/library") || path.startsWith("/api/") || path.startsWith("/files/")) library.handle(exchange);
            else shop.handle(exchange);
        } catch (IllegalArgumentException exception) {
            Http.send(exchange, 400, "text/plain", exception.getMessage() == null ? "Проверьте данные." : exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace(); Http.send(exchange, 500, "text/plain", "Не удалось выполнить запрос. Проверьте подключение к базе.");
        }
    }
    private String token(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) for (String part : cookie.split(";")) {
            String value = part.strip();
            if (value.startsWith("shelf_session=")) return value.substring("shelf_session=".length());
        }
        return null;
    }
}
