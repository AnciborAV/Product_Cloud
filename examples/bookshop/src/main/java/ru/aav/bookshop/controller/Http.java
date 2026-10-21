package ru.aav.bookshop.controller;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Общие небольшие операции HTTP: UTF-8, безопасные заголовки и ограниченные формы. */
public final class Http {
    /**
     * Запрещает создавать экземпляры служебного класса, содержащего только статические операции.
     */
    private Http() {}

    /** Кодирует текст в UTF-8 и передаёт отправку байтов общему методу. */
    public static void send(HttpExchange exchange, int status, String type, String body)
            throws IOException {
        bytes(exchange, status, type + "; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Устанавливает тип содержимого и защитные заголовки, отправляет тело и закрывает HTTP-обмен.
     */
    public static void bytes(HttpExchange exchange, int status, String type, byte[] body)
            throws IOException {
        // Заголовки HTTP-ответа, включая тип содержимого и правила безопасности.
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", type);
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set(
                "Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none';"
                        + " base-uri 'none'; frame-ancestors 'self'");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    /** Отправляет HTTP 303: после изменяющего запроса браузер выполняет безопасный GET. */
    public static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    /** Читает форму с ограничением 4096 байт и декодирует URL-encoded параметры. */
    public static Map<String, String> form(HttpExchange exchange) throws IOException {
        return decode(new String(read(exchange, 4096), StandardCharsets.UTF_8));
    }

    /** Разбирает пары ключ=значение и URL-кодирование; некорректные пары отклоняет. */
    public static Map<String, String> decode(String body) {
        // Коллекция результатов, заполняемая данными текущего запроса.
        Map<String, String> result = new HashMap<>();
        if (body == null || body.isEmpty()) return result;
        for (String pair : body.split("&")) {
            // Пара ключ/значение после разделения одного параметра формы.
            String[] pieces = pair.split("=", 2);
            if (pieces.length != 2) throw new IllegalArgumentException("Некорректные параметры.");
            result.put(
                    URLDecoder.decode(pieces[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pieces[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    /**
     * Читает максимум limit + 1 байт, чтобы обнаружить превышение лимита без неограниченной
     * загрузки в память.
     */
    public static byte[] read(HttpExchange exchange, int limit) throws IOException {
        // Тело запроса с проверенным ограничением размера.
        byte[] body = exchange.getRequestBody().readNBytes(limit + 1);
        if (body.length > limit)
            throw new IllegalArgumentException("Файл или запрос превышает допустимый размер.");
        return body;
    }
}
