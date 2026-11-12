# Развёртывание Product Cloud на Render

## Состав

Отдельный Web Service `product-cloud` и отдельная PostgreSQL 17 `product-cloud-db`.
Существующие Book_Store и OPORA не меняются. Образ собирается Dockerfile на Java 25;
процесс работает без root, секреты берутся только из окружения. Flyway создаст схему
и начальный ассортимент согласно текущим миграциям приложения.

## Через Blueprint

1. Примите PR в ветку `main`.
2. Render → New → Blueprint → подключите `AnciborAV/Product_Cloud`, ветку `main`.
3. Проверьте `render.yaml`, регион Frankfurt и планы Free для обеих служб.
4. Подтвердите создание. После успешной сборки Web Service получит адрес onrender.com.
5. Зарегистрируйте новый аккаунт. Локальные пользователи и данные сами не переносятся.

Free PostgreSQL в Render истекает через 30 дней. Free Web Service засыпает при
простоях. Если Free недоступен в вашем рабочем пространстве, не подтверждайте
платный план автоматически: сначала выберите условия хранения данных и оплаты.
После первого запуска автоматическое развёртывание отключено: новые версии
запускаются через Manual Deploy → Deploy latest commit.

## Если создавать через Dashboard вручную

Создайте отдельный Project `Product Cloud`, внутри него PostgreSQL 17 и Web Service
из этого репозитория. Обе службы должны быть в одном регионе. Для Web Service:

| Поле | Значение |
| --- | --- |
| Branch | main |
| Language / Runtime | Docker |
| Root Directory | пусто |
| Dockerfile Path | ./Dockerfile |
| Health Check Path | / |
| Instance Type | Free, если доступен |

Передайте переменные в Environment Web Service:

| Переменная | Значение |
| --- | --- |
| SPRING_PROFILES_ACTIVE | render |
| DB_HOST | внутренний Hostname новой PostgreSQL |
| DB_PORT | 5432 |
| DB_NAME | Database новой PostgreSQL |
| DB_USERNAME | Username новой PostgreSQL |
| DB_PASSWORD | Password новой PostgreSQL |
| MAIL_ENABLED | false для первого запуска |

PORT задаётся самим Render. Профиль `render` использует HTTPS Secure-cookie и
перенаправленные заголовки прокси. Локальный `.env.smtp.properties` в образ не
попадает. Вне профиля render локальное подключение использует DB_URL, DB_USERNAME
и DB_PASSWORD. Укажите локальные параметры в конфигурации запуска IDEA.

Фото профиля и другие пользовательские данные хранятся в PostgreSQL. Смена
Web Service или его пересборка не переносит локальную базу и не заменяет резервные копии.

## Почта

Сначала проверьте сайт с MAIL_ENABLED=false. Затем задайте в Environment действующие
MAIL_FROM, SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD и флаги TLS из README.
После этого включите MAIL_ENABLED=true и перезапустите сервис. Не копируйте пароли
в Git или переписку. На Free Web Service исходящие SMTP-порты 25, 465 и 587 заблокированы Render.
Для SMTP на 465/587 нужен подходящий платный Web Service; другой вариант —
отдельная реализация отправки через почтовый HTTP API. Одна смена SMTP-пароля
это ограничение не устраняет.

## Проверка и ошибки

CI запускает полный Maven verify и собирает Docker image на Java 25. На Render
дождитесь Live, откройте главную, проверьте регистрацию, вход, подборку и аватар.
Если миграции не проходят, проверьте, что подключена отдельная новая база. Если
проверка порта не проходит — профиль должен быть render, адрес 0.0.0.0, порт PORT.
Не используйте рабочие базы Book_Store или OPORA для тестов или новых миграций.

Документация: https://render.com/docs/docker,
https://render.com/docs/blueprint-spec, https://render.com/docs/free.
