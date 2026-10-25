CREATE TABLE products (
 id UUID PRIMARY KEY, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 version BIGINT NOT NULL DEFAULT 0, name VARCHAR(100) NOT NULL,
 price NUMERIC(8,2) NOT NULL CHECK(price > 0), category VARCHAR(30) NOT NULL,
 unit VARCHAR(30) NOT NULL
);
CREATE TABLE user_accounts (
 id UUID PRIMARY KEY, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 version BIGINT NOT NULL DEFAULT 0, username VARCHAR(50) NOT NULL UNIQUE,
 display_name VARCHAR(100) NOT NULL, password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK(role IN ('USER','ADMIN')),
 avatar BYTEA, avatar_content_type VARCHAR(30)
);
CREATE TABLE selected_products (
 id UUID PRIMARY KEY, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
 product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
 UNIQUE(user_id,product_id)
);
CREATE TABLE reviews (
 id UUID PRIMARY KEY, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
 product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
 rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5), text VARCHAR(2000) NOT NULL,
 UNIQUE(user_id,product_id)
);
CREATE INDEX reviews_created_idx ON reviews(created_at DESC);
CREATE INDEX selected_user_created_idx ON selected_products(user_id,created_at DESC);
CREATE INDEX reviews_user_created_idx ON reviews(user_id,created_at DESC);
