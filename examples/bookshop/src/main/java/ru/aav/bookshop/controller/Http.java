package ru.aav.bookshop.controller;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Общие небольшие операции HTTP: UTF-8, безопасные заголовки и ограниченные формы. */
public final class Http {
    private Http() {}
    public static void send(HttpExchange exchange, int status, String type, String body) throws IOException {
        bytes(exchange, status, type + "; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }
    public static void bytes(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", type); headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff"); headers.set("Referrer-Policy", "no-referrer");
        headers.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'self'");
        exchange.sendResponseHeaders(status, body.length); exchange.getResponseBody().write(body); exchange.close();
    }
    public static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(303, -1); exchange.close();
    }
    public static Map<String, String> form(HttpExchange exchange) throws IOException {
        return decode(new String(read(exchange, 4096), StandardCharsets.UTF_8));
    }
    public static Map<String, String> decode(String body) {
        Map<String, String> result = new HashMap<>();
        if (body == null || body.isEmpty()) return result;
        for (String pair : body.split("&")) {
            String[] pieces = pair.split("=", 2);
            if (pieces.length != 2) throw new IllegalArgumentException("Некорректные параметры.");
            result.put(URLDecoder.decode(pieces[0], StandardCharsets.UTF_8), URLDecoder.decode(pieces[1], StandardCharsets.UTF_8));
        }
        return result;
    }
    public static byte[] read(HttpExchange exchange, int limit) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(limit + 1);
        if (body.length > limit) throw new IllegalArgumentException("Файл или запрос превышает допустимый размер.");
        return body;
    }
}
