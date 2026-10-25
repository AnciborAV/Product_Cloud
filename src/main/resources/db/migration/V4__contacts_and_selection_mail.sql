ALTER TABLE user_accounts ADD COLUMN last_name VARCHAR(100);
ALTER TABLE user_accounts ADD COLUMN phone VARCHAR(30);
ALTER TABLE user_accounts ADD COLUMN email VARCHAR(254);
ALTER TABLE selected_products ADD COLUMN product_name VARCHAR(100);
UPDATE selected_products SET product_name = (SELECT name FROM products WHERE products.id = selected_products.product_id);
ALTER TABLE selected_products ALTER COLUMN product_name SET NOT NULL;
CREATE TABLE selection_mail_outbox (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
 recipient VARCHAR(254) NOT NULL,
 body TEXT NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 sent_at TIMESTAMP WITH TIME ZONE,
 attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX selection_mail_pending_idx ON selection_mail_outbox(sent_at, created_at);
