-- 회원당 하나의 모의투자 계좌. 장부는 USD 로만 기록한다.
CREATE TABLE invest_accounts (
    id          UUID           NOT NULL,
    user_id     UUID           NOT NULL,
    cash        DECIMAL(18, 2) NOT NULL,
    seed_money  DECIMAL(18, 2) NOT NULL,
    created_at  DATETIME(6)    NULL,
    updated_at  DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invest_accounts_user UNIQUE (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
