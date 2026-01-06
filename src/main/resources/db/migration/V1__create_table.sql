CREATE TABLE authors
(
    id         BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    name       VARCHAR(255) NULL,
    CONSTRAINT pk_authors PRIMARY KEY (id)
);

CREATE TABLE cart_items
(
    id         BIGINT NOT NULL,
    cart_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NULL,
    total      BIGINT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (id)
);

CREATE TABLE carts
(
    id      BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total   BIGINT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id)
);

CREATE TABLE categories
(
    id        BIGINT       NOT NULL,
    name      VARCHAR(255) NOT NULL,
    parent_id BIGINT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE coupons
(
    id             BIGINT       NOT NULL,
    code           VARCHAR(50)  NOT NULL,
    `description`  VARCHAR(255) NULL,
    created_at     datetime     NOT NULL,
    updated_at     datetime     NOT NULL,
    stock_quantity INT          NOT NULL,
    discount_type  VARCHAR(255) NOT NULL,
    discount       INT          NOT NULL,
    CONSTRAINT pk_coupons PRIMARY KEY (id)
);

CREATE TABLE invalid_tokens
(
    token      VARCHAR(255) NOT NULL,
    expired_at datetime NULL,
    CONSTRAINT pk_invalid_tokens PRIMARY KEY (token)
);

CREATE TABLE order_coupons
(
    id        BIGINT NOT NULL,
    order_id  BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    CONSTRAINT pk_order_coupons PRIMARY KEY (id)
);

CREATE TABLE order_items
(
    id         BIGINT NOT NULL,
    order_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NULL,
    total      BIGINT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id)
);

CREATE TABLE orders
(
    id             BIGINT       NOT NULL,
    customer_id    BIGINT       NOT NULL,
    address        VARCHAR(255) NOT NULL,
    created_at     datetime     NOT NULL,
    updated_at     datetime     NOT NULL,
    status         VARCHAR(255) NULL,
    method_payment VARCHAR(255) NULL,
    total_amount   BIGINT NULL,
    total_item     INT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE password_reset_tokens
(
    id         VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at datetime     NOT NULL,
    created_at datetime NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id)
);

CREATE TABLE permissions
(
    id            BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) NULL,
    created_at    datetime     NOT NULL,
    CONSTRAINT pk_permissions PRIMARY KEY (id)
);

CREATE TABLE product_images
(
    id         BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    public_id  VARCHAR(255) NULL,
    image_url  VARCHAR(255) NULL,
    CONSTRAINT pk_product_images PRIMARY KEY (id)
);

CREATE TABLE product_previews
(
    id         BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    image_url  VARCHAR(255) NULL,
    CONSTRAINT pk_product_previews PRIMARY KEY (id)
);

CREATE TABLE products
(
    id                BIGINT       NOT NULL,
    category_id       BIGINT       NOT NULL,
    name              VARCHAR(255) NOT NULL,
    short_description TEXT NULL,
    `description`     TEXT NULL,
    dimension         VARCHAR(255) NULL,
    number_of_pages   INT NULL,
    isbn              VARCHAR(100) NOT NULL,
    stock_quantity    INT          NULL,
    price             BIGINT       NOT NULL,
    discount          INT          NULL,
    publisher         VARCHAR(255) NOT NULL,
    publisher_date    datetime     NOT NULL,
    rating_avg DOUBLE NULL,
    rating_count      INT NULL,
    created_at        datetime     NOT NULL,
    updated_at        datetime     NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id)
);

CREATE TABLE refresh_tokens
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    token      VARCHAR(512) NOT NULL,
    user_id    BIGINT       NOT NULL,
    expired_at datetime     NOT NULL,
    revoked    BIT(1)       NOT NULL,
    created_at datetime     NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

CREATE TABLE reviews
(
    id            BIGINT   NOT NULL,
    order_item_id BIGINT   NOT NULL,
    product_id    BIGINT   NOT NULL,
    created_at    datetime NOT NULL,
    rating        INT NULL,
    comment       VARCHAR(1000) NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id)
);

CREATE TABLE role_permissions
(
    permission_id BIGINT NOT NULL,
    role_id       BIGINT NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (permission_id, role_id)
);

CREATE TABLE roles
(
    id            BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    `description` VARCHAR(500) NULL,
    created_at    datetime     NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE user_roles
(
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (role_id, user_id)
);

CREATE TABLE users
(
    id             BIGINT       NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    avatar_url     VARCHAR(255) NULL,
    phone          VARCHAR(20) NULL,
    address        VARCHAR(255) NULL,
    google_id      VARCHAR(255) NULL,
    is_active      BIT(1)       NOT NULL,
    is_locked      BIT(1)       NOT NULL,
    mfa_enabled    BIT(1)       NOT NULL,
    otp            VARCHAR(6) NULL,
    otp_expires_at datetime NULL,
    otp_consumed   BIT(1)       NOT NULL,
    last_login_at  datetime NULL,
    created_at     datetime     NOT NULL,
    updated_at     datetime     NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE wishlists
(
    id         BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT pk_wishlists PRIMARY KEY (id)
);

ALTER TABLE carts
    ADD CONSTRAINT uc_carts_user UNIQUE (user_id);

ALTER TABLE coupons
    ADD CONSTRAINT uc_coupons_code UNIQUE (code);

ALTER TABLE password_reset_tokens
    ADD CONSTRAINT uc_password_reset_tokens_token UNIQUE (token);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_google UNIQUE (google_id);

ALTER TABLE authors
    ADD CONSTRAINT FK_AUTHORS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE carts
    ADD CONSTRAINT FK_CARTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE cart_items
    ADD CONSTRAINT FK_CART_ITEMS_ON_CART FOREIGN KEY (cart_id) REFERENCES carts (id);

ALTER TABLE cart_items
    ADD CONSTRAINT FK_CART_ITEMS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE orders
    ADD CONSTRAINT FK_ORDERS_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES users (id);

ALTER TABLE order_coupons
    ADD CONSTRAINT FK_ORDER_COUPONS_ON_COUPON FOREIGN KEY (coupon_id) REFERENCES coupons (id);

ALTER TABLE order_coupons
    ADD CONSTRAINT FK_ORDER_COUPONS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE order_items
    ADD CONSTRAINT FK_ORDER_ITEMS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE order_items
    ADD CONSTRAINT FK_ORDER_ITEMS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE products
    ADD CONSTRAINT FK_PRODUCTS_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES categories (id);

ALTER TABLE product_images
    ADD CONSTRAINT FK_PRODUCT_IMAGES_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE product_previews
    ADD CONSTRAINT FK_PRODUCT_PREVIEWS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE reviews
    ADD CONSTRAINT FK_REVIEWS_ON_ORDER_ITEM FOREIGN KEY (order_item_id) REFERENCES order_items (id);

ALTER TABLE reviews
    ADD CONSTRAINT FK_REVIEWS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE wishlists
    ADD CONSTRAINT FK_WISHLISTS_ON_PRODUCT FOREIGN KEY (product_id) REFERENCES products (id);

ALTER TABLE wishlists
    ADD CONSTRAINT FK_WISHLISTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE role_permissions
    ADD CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE role_permissions
    ADD CONSTRAINT fk_role_permissions_rolePwOXJt FOREIGN KEY (permission_id) REFERENCES permissions (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_userntIt6c FOREIGN KEY (role_id) REFERENCES roles (id);