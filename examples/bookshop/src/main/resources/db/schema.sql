-- Выполняется автоматически при старте в выделенной базе quiet_shelf.
CREATE TABLE IF NOT EXISTS books (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    color VARCHAR(20) NOT NULL CHECK (color IN ('olive','terracotta','navy','plum','ochre','sage')),
    description TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE IF NOT EXISTS order_items (
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id),
    title VARCHAR(200) NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    PRIMARY KEY (order_id, book_id)
);
INSERT INTO books(id,title,author,category,price,color,description) VALUES
(1,'Так говорил Заратустра','Фридрих Ницше','Философия',890.00,'olive','Книга о свободе, преодолении себя и поиске собственного пути.'),
(2,'Посторонний','Альбер Камю','Проза',640.00,'terracotta','Лаконичный роман о человеке и мире, который не даёт готовых ответов.'),
(3,'Размышления','Марк Аврелий','Философия',720.00,'navy','Личные записи о стойкости, внимании и внутренней независимости.'),
(4,'Процесс','Франц Кафка','Проза',690.00,'plum','Человек перед загадочной системой — классика европейского модернизма.'),
(5,'Идиот','Фёдор Достоевский','Классика',980.00,'ochre','Роман о сострадании, красоте и противоречиях человеческой природы.'),
(6,'Вишнёвый сад','Антон Чехов','Классика',480.00,'sage','Пьеса о времени, памяти и переменах, к которым невозможно подготовиться.')
ON CONFLICT (id) DO NOTHING;

-- Единственный владелец локальной библиотеки. Пароль хранится только как соль и PBKDF2-хеш.
CREATE TABLE IF NOT EXISTS app_user (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    email VARCHAR(254) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL
);
CREATE TABLE IF NOT EXISTS shelves (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) UNIQUE NOT NULL
);
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    shelf_id BIGINT NOT NULL REFERENCES shelves(id),
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content BYTEA NOT NULL CHECK (octet_length(content) BETWEEN 5 AND 20971520),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS documents_shelf_idx ON documents(shelf_id);
