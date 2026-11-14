CREATE TABLE invest_trades (
    id          UUID           NOT NULL,
    account_id  UUID           NOT NULL,
    symbol      VARCHAR(20)    NOT NULL,
    side        VARCHAR(10)    NOT NULL,
    quantity    BIGINT         NOT NULL,
    price       DECIMAL(18, 4) NOT NULL,
    -- quantity * price. 조회 편의를 위해 저장한다.
    amount      DECIMAL(18, 2) NOT NULL,
    created_at  DATETIME(6)    NULL,
    updated_at  DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invest_trades_account
        FOREIGN KEY (account_id) REFERENCES invest_accounts (id)
        ON DELETE CASCADE,
    -- 내 거래 내역 최신순
    INDEX idx_invest_trades_account_created (account_id, created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
