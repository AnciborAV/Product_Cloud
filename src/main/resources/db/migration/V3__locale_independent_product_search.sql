ALTER TABLE products ADD COLUMN search_name VARCHAR(100);
UPDATE products SET search_name = lower(translate(name,
 'АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ',
 'абвгдеёжзийклмнопрстуфхцчшщъыьэюя'));
ALTER TABLE products ALTER COLUMN search_name SET NOT NULL;
