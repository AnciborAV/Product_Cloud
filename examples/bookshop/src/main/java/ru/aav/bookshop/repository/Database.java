package ru.aav.bookshop.repository;

import java.sql.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Настройки подключения берутся из окружения: пароль не попадает в исходный код. */
public final class Database {
    private final String url, user, password;
    public Database(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }
    public static Database fromEnvironment() {
        String password = System.getenv("SHOP_DB_PASSWORD");
        if (password == null || password.isBlank())
            throw new IllegalStateException("Укажите SHOP_DB_PASSWORD в переменных окружения запуска.");
        return new Database(System.getenv().getOrDefault("SHOP_DB_URL", "jdbc:postgresql://localhost:5432/quiet_shelf"),
            System.getenv().getOrDefault("SHOP_DB_USER", "postgres"), password);
    }
    /** Каждый запрос получает своё соединение; try-with-resources закрывает его после работы. */
    public Connection connect() throws SQLException { return DriverManager.getConnection(url, user, password); }

    /** Создаёт учебную схему и добавляет книги один раз, не перезаписывая существующие данные. */
    public void initialize() throws SQLException, IOException {
        try (var input = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (input == null) throw new IOException("Не найден db/schema.sql");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (var connection = connect()) {
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    statement.execute(sql);
                    connection.commit();
                } catch (SQLException failure) { connection.rollback(); throw failure; }
            }
        }
    }
}
