-- 사용자별 관심 종목. 주간 정산에도 지우지 않는다.
CREATE TABLE invest_watchlist (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     UUID        NOT NULL,
    symbol      VARCHAR(20) NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invest_watchlist UNIQUE (user_id, symbol),
    INDEX idx_invest_watchlist_symbol (symbol)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
