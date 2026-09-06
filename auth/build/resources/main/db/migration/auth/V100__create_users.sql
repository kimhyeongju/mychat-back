CREATE TABLE users (
    id            UUID         NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password      VARCHAR(255) NOT NULL,
    nickname      VARCHAR(30)  NOT NULL,
    phone_number  VARCHAR(20)  NOT NULL,
    email         VARCHAR(255) NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NULL,
    updated_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username     UNIQUE (username),
    CONSTRAINT uk_users_nickname     UNIQUE (nickname),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;