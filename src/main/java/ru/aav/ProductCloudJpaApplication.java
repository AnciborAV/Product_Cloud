package ru.aav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа в веб-приложение Product Cloud. Spring Boot создаёт контейнер объектов (контекст),
 * настраивает инфраструктуру и запускает встроенный HTTP-сервер. @SpringBootApplication объединяет
 * конфигурацию, автоконфигурацию и поиск компонентов в ru.aav и его подпакетах.
 */
@SpringBootApplication
public class ProductCloudJpaApplication {

    /**
     * Запускает приложение; args передаёт параметры командной строки Spring Boot. Например,
     * --spring.profiles.active=demo выбирает локальную базу H2.
     */
    public static void main(String[] args) {
        SpringApplication.run(ProductCloudJpaApplication.class, args);
    }
}
