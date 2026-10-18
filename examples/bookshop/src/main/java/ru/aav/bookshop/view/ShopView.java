package ru.aav.bookshop.view;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import ru.aav.bookshop.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Thymeleaf объединяет HTML-шаблон с моделью представления. th:text экранирует текст. */
public final class ShopView {
    private final TemplateEngine engine;
    public ShopView() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("web/"); resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML"); resolver.setCharacterEncoding("UTF-8");
        engine = new TemplateEngine(); engine.setTemplateResolver(resolver);
    }
    public String catalog(List<Book> books) {
        var context = new Context(Locale.forLanguageTag("ru"));
        context.setVariable("books", books.stream().map(BookView::from).toList());
        return engine.process("index", context);
    }
    public String receipt(Order order) {
        var context = new Context(Locale.forLanguageTag("ru"));
        context.setVariable("order", order);
        context.setVariable("total", BookView.money(order.total()));
        return engine.process("receipt", context);
    }
    public String login(boolean setup) {
        var context = new Context(Locale.forLanguageTag("ru"));
        context.setVariable("setup", setup);
        return engine.process("login", context);
    }
    public String library(List<ru.aav.bookshop.repository.LibraryRepository.Shelf> shelves,
                          List<LibraryDocument> documents) {
        var context = new Context(Locale.forLanguageTag("ru"));
        context.setVariable("shelves", shelves); context.setVariable("documents", documents);
        return engine.process("library", context);
    }
    public String error(String message) {
        var context = new Context(Locale.forLanguageTag("ru"));
        context.setVariable("message", message);
        return engine.process("error", context);
    }
    /** CSS и JS отдаются как ресурсы, без обработки шаблонизатором. */
    public static String resource(String name) throws IOException {
        try (var input = ShopView.class.getResourceAsStream("/web/" + name)) {
            if (input == null) throw new IOException("Resource missing: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
