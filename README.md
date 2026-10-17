# Product Cloud

Учебный каталог товаров: Java 25 LTS, Spring Boot 4.1.1, Spring MVC,
Thymeleaf, JPA и Maven. Сохраняет исходный сценарий просмотра и добавления товаров.

## Быстрый запуск без PostgreSQL

```sh
./mvnw clean verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Откройте http://localhost:8080/products. Профиль demo сохраняет H2 в `data/`.
В Windows замените `./mvnw` на `mvnw.cmd`. Нужен JDK 25.

## PostgreSQL

Создайте базу products. Укажите DB_PASSWORD; DB_USERNAME по умолчанию postgres,
DB_URL — jdbc:postgresql://localhost:5432/products.

```sh
export DB_PASSWORD='your-local-password'
./mvnw spring-boot:run
```

Приложение слушает только localhost. SERVER_ADDRESS позволяет явно изменить адрес.
Это локальное учебное приложение без авторизации; не публикуйте его в интернете
как готовый многопользовательский сервис.

## Изменения

- BigDecimal и numeric(19,2) вместо double: цены не теряют десятичную точность.
- Проверки названия, диапазона и точности цены на форме и в модели.
- Constructor injection, транзакции сервиса и неизменяемые ProductView.
- Обновление/удаление отсутствующего товара дают ProductNotFoundException.
- Post/Redirect/Get предотвращает повторную отправку формы при обновлении страницы.
- Пагинация по 20 товаров, стабильная сортировка, адаптивная страница и ошибки рядом с полями.
- Удалены Lombok, MySQL-драйвер и IDE-файлы. Используется один понятный стек.

## Данные и проверка

Сохранены таблица products, Integer id и последовательность products_seq (шаг 50).
Для учебной разработки оставлен Hibernate ddl-auto=update. Перед подключением
старой PostgreSQL-базы сделайте резервную копию: переход double → numeric(19,2)
может округлить старые цены, а некорректные/пустые значения нужно исправить заранее.
Миграция произвольной существующей базы автоматически не гарантируется.
Для промышленной эксплуатации нужны версионируемые миграции и авторизация.

`./mvnw clean verify` запускает интеграционные тесты с отдельной H2-базой.
GitHub Actions запускает тот же набор на PostgreSQL 16 через TEST_DB_URL,
TEST_DB_USERNAME, TEST_DB_PASSWORD. Тестовый профиль использует create-drop:
не указывайте в TEST_DB_URL базу с нужными данными.
