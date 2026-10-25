package org.example.aav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Этап 1: создаём точку запуска, чтобы Spring Boot мог запустить магазин. Учебная последовательность. */
@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class SpringDemoApplication {

    /**
     * Запускает Spring Boot: создаёт объекты приложения и запускает веб-сервер.
     * @param args параметры командной строки, переданные при запуске
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringDemoApplication.class, args);
    }
}
