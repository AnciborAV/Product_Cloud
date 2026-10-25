ALTER TABLE selection_mail_outbox ADD COLUMN subject VARCHAR(200) NOT NULL DEFAULT 'Зелёная лавка — ваш заказ';
ALTER TABLE selection_mail_outbox ADD COLUMN kind VARCHAR(30) NOT NULL DEFAULT 'ORDER';
ALTER TABLE selection_mail_outbox ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
CREATE TABLE password_changes (
 user_id UUID PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
 recipient VARCHAR(254) NOT NULL,
 code_hash VARCHAR(64) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0,
 requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
 expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
