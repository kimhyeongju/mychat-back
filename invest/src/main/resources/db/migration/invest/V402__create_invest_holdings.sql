CREATE TABLE invest_holdings (
    id          UUID           NOT NULL,
    account_id  UUID           NOT NULL,
    symbol      VARCHAR(20)    NOT NULL,
    quantity    BIGINT         NOT NULL,
    -- 매수 평균 단가. 매도해도 바뀌지 않는다.
    avg_cost    DECIMAL(18, 4) NOT NULL,
    created_at  DATETIME(6)    NULL,
    updated_at  DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invest_holdings_account
        FOREIGN KEY (account_id) REFERENCES invest_accounts (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_invest_holdings_account_symbol UNIQUE (account_id, symbol),
    -- 보유 중인 종목 목록(시세 갱신 대상) 조회
    INDEX idx_invest_holdings_symbol (symbol)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
