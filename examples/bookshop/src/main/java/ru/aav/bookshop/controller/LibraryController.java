package ru.aav.bookshop.controller;

import com.sun.net.httpserver.*;

import ru.aav.bookshop.service.LibraryService;
import ru.aav.bookshop.view.ShopView;

import java.io.IOException;
import java.util.UUID;

/** Закрытые маршруты полок, загрузки, чтения и скачивания PDF. Вход проверяет AuthController. */
public final class LibraryController implements HttpHandler {
    /** Зависимость для операций личной библиотеки. */
    private final LibraryService library;

    /** Компонент формирования HTML-представлений. */
    private final ShopView view;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public LibraryController(LibraryService library, ShopView view) {
        this.library = library;
        this.view = view;
    }

    /**
     * Обрабатывает список полок, загрузку и выдачу PDF, перемещение документов; доступ
     * предварительно проверяет AuthController. {@code @Override} позволяет компилятору проверить,
     * что метод переопределяет существующий контракт.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Путь URL, по которому выбирается обработчик; это не путь к файлу на диске.
            String path = exchange.getRequestURI().getPath();
            // HTTP-метод запроса: определяет допустимое действие маршрута.
            String method = exchange.getRequestMethod();
            if (method.equals("GET") && path.equals("/library")) {
                Http.send(
                        exchange,
                        200,
                        "text/html",
                        view.library(library.shelves(), library.documents()));
            } else if (method.equals("GET") && path.matches("/files/[0-9a-fA-F-]{36}")) {
                // Идентификатор документа, используемый для точного обращения к записи.
                UUID id = UUID.fromString(path.substring(7));
                // Результат поиска содержимого PDF; Optional позволяет отдельно обработать
                // отсутствие файла.
                var content = library.content(id);
                if (content.isEmpty()) {
                    Http.send(exchange, 404, "text/html", view.error("Книга не найдена."));
                    return;
                }
                // Флаг выдачи PDF как вложения вместо показа в браузере.
                boolean download =
                        "1"
                                .equals(
                                        Http.decode(exchange.getRequestURI().getRawQuery())
                                                .get("download"));
                exchange.getResponseHeaders()
                        .set(
                                "Content-Disposition",
                                (download ? "attachment" : "inline")
                                        + "; filename=\"book-"
                                        + id
                                        + ".pdf\"");
                Http.bytes(exchange, 200, "application/pdf", content.get());
            } else if (method.equals("POST") && path.equals("/api/shelves")) {
                library.createShelf(Http.form(exchange).get("name"));
                Http.send(exchange, 201, "text/plain", "/library");
            } else if (method.equals("POST") && path.equals("/api/documents")) {
                // Параметры URL, которые валидируются до записи документа.
                var parameters = Http.decode(exchange.getRequestURI().getRawQuery());
                library.upload(
                        Long.parseLong(parameters.getOrDefault("shelf", "0")),
                        parameters.get("title"),
                        parameters.get("author"),
                        parameters.get("filename"),
                        Http.read(exchange, LibraryService.MAX_PDF_BYTES));
                Http.send(exchange, 201, "text/plain", "/library");
            } else if (method.equals("POST") && path.equals("/api/move")) {
                // Разобранные поля HTTP-формы; значения ещё нужно проверить перед действием.
                var form = Http.form(exchange);
                // Результат переноса: false означает, что документ не найден.
                boolean moved =
                        library.move(
                                UUID.fromString(form.getOrDefault("id", "")),
                                Long.parseLong(form.get("shelf")));
                Http.send(
                        exchange,
                        moved ? 200 : 404,
                        "text/plain",
                        moved ? "/library" : "Книга не найдена.");
            } else Http.send(exchange, 404, "text/html", view.error("Страница не найдена."));
        } catch (IllegalArgumentException exception) {
            Http.send(
                    exchange,
                    400,
                    "text/plain",
                    exception.getMessage() == null
                            ? "Проверьте введённые данные."
                            : exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace();
            Http.send(
                    exchange,
                    500,
                    "text/plain",
                    "Не удалось выполнить действие. Проверьте журнал сервера.");
        }
    }
}
