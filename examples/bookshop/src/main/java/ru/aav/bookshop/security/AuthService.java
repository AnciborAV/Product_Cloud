package ru.aav.bookshop.security;

import ru.aav.bookshop.repository.Database;

import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Один владелец, непрозрачные сессии на 8 часов и ограничение частоты неверных входов. */
public final class AuthService {
    /** Настройки и фабрика JDBC-соединений. */
    private final Database database;

    /**
     * Потокобезопасная таблица токенов и сроков их действия; после перезапуска вход требуется
     * повторно.
     */
    private final Map<String, Instant> sessions = new ConcurrentHashMap<>();

    /** Очередь времени неудачных входов для ограничения частоты; доступ защищён synchronized. */
    private final ArrayDeque<Instant> failures = new ArrayDeque<>();

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public AuthService(Database database) {
        this.database = database;
    }

    /** Проверяет наличие единственного владельца приложения в таблице app_user. */
    public boolean configured() throws SQLException {
        try (var connection = database.connect();
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT 1 FROM app_user WHERE id=1")) {
            return rows.next();
        }
    }

    /**
     * Первичная настройка разрешена только до создания владельца. PK не допускает второго аккаунта.
     */
    public void setup(String email, String password) throws SQLException {
        email = normalize(email);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || email.length() > 254
                || password.length() < 4
                || password.length() > 128)
            throw new IllegalArgumentException(
                    "Введите email и пароль длиной от 4 до 128 символов.");
        // Хеш пароля с солью; исходный пароль не сохраняется.
        String hash = Passwords.hash(password);
        try (var connection = database.connect();
                var statement =
                        connection.prepareStatement(
                                "INSERT INTO app_user(id,email,password_hash) VALUES (1,?,?) ON"
                                        + " CONFLICT (id) DO NOTHING")) {
            statement.setString(1, email);
            statement.setString(2, hash);
            if (statement.executeUpdate() != 1)
                throw new IllegalArgumentException("Владелец уже создан. Используйте вход.");
        }
    }

    /**
     * Проверяет email и пароль, ограничивает частоту ошибок и создаёт случайную восьмичасовую
     * сессию; synchronized защищает очередь попыток.
     */
    public synchronized Optional<String> login(String email, String password) throws SQLException {
        // Единый момент времени для проверки попыток входа и срока создаваемой сессии.
        Instant now = Instant.now();
        while (!failures.isEmpty() && failures.peek().isBefore(now.minusSeconds(60)))
            failures.remove();
        if (failures.size() >= 5)
            throw new IllegalArgumentException("Слишком много попыток. Подождите минуту.");
        // Итог проверки учётных данных; отказ не раскрывает, какое именно поле неверно.
        boolean valid = false;
        try (var connection = database.connect();
                var statement =
                        connection.prepareStatement(
                                "SELECT email,password_hash FROM app_user WHERE id=1");
                var rows = statement.executeQuery()) {
            if (rows.next()) {
                // Результат проверки пароля по сохранённому хешу.
                boolean passwordValid = Passwords.verify(password, rows.getString("password_hash"));
                valid = rows.getString("email").equals(normalize(email)) && passwordValid;
            }
        }
        if (!valid) {
            failures.add(now);
            return Optional.empty();
        }
        failures.clear();
        sessions.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        // Буфер байтов текущей операции; после подготовки преобразуется в ответ или сохраняемое
        // значение.
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        // Случайный токен для идентификации сессии или предотвращения повторного оформления.
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, now.plus(Duration.ofHours(8)));
        return Optional.of(token);
    }

    /** Проверяет существование и срок действия сессии; истёкший токен удаляет. */
    public boolean authenticated(String token) {
        if (token == null) return false;
        // Срок действия найденной сессии; null означает неизвестный токен.
        Instant expires = sessions.get(token);
        if (expires == null) return false;
        if (expires.isBefore(Instant.now())) {
            sessions.remove(token);
            return false;
        }
        return true;
    }

    /** Удаляет токен из хранилища сессий, немедленно прекращая его действие. */
    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    /** Приводит email к единому регистру и удаляет пробелы по краям. */
    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
