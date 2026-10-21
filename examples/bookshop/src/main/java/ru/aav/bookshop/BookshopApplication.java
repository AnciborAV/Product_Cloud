package ru.aav.bookshop;

import com.sun.net.httpserver.HttpServer;

import ru.aav.bookshop.controller.*;
import ru.aav.bookshop.repository.*;
import ru.aav.bookshop.security.AuthService;
import ru.aav.bookshop.service.LibraryService;
import ru.aav.bookshop.service.ShopService;
import ru.aav.bookshop.view.ShopView;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/** Точка входа и сборка зависимостей вручную: здесь хорошо видно, кто от кого зависит. */
public final class BookshopApplication {
    /**
     * Создаёт зависимости вручную, подготавливает базу и запускает локальный HTTP-сервер; shutdown
     * hook останавливает сервер и пул потоков.
     */
    public static void main(String[] args) throws Exception {
        // Порт локального сервера; по умолчанию 8081.
        int port = Integer.parseInt(System.getProperty("shop.port", "8081"));
        // Объект подключения к базе с настройками из окружения.
        Database database = Database.fromEnvironment();
        database.initialize();
        // Сервис магазина с репозиториями каталога и заказов.
        var service = new ShopService(new BookRepository(database), new OrderRepository(database));
        // Пример предназначен для локального обучения и слушает только loopback-интерфейс.
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 32);
        // Пул потоков HTTP-сервера; завершается при остановке приложения.
        var executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        // Настроенный компонент рендеринга HTML.
        var view = new ShopView();
        // Сервис личной PDF-библиотеки с отдельным репозиторием.
        var library = new LibraryService(new LibraryRepository(database));
        server.createContext(
                "/",
                new AuthController(
                        new AuthService(database),
                        new ShopController(service, view),
                        new LibraryController(library, view),
                        view));
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    server.stop(1);
                                    executor.shutdown();
                                }));
        server.start();
        System.out.println("Тихая полка: http://localhost:" + port);
        System.out.println("Каталог и заказы хранятся в PostgreSQL.");
    }
}
