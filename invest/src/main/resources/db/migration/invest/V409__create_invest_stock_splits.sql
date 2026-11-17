-- 주식 분할/병합 일정. 시세 제공자의 기업 이벤트에서 가져온다.
-- new_rate / old_rate 는 "old_rate 주가 new_rate 주가 된다"는 뜻이다. (4:1 분할 → new 4, old 1 / 1:10 병합 → new 1, old 10)
CREATE TABLE invest_stock_splits (
    id              UUID           NOT NULL,
    symbol          VARCHAR(20)    NOT NULL,
    ex_date         DATE           NOT NULL,
    new_rate        DECIMAL(18, 6) NOT NULL,
    old_rate        DECIMAL(18, 6) NOT NULL,
    -- ex_date 의 첫 분할 후 체결이 나온 뒤에야 알게 된 경우 true. 그 사이 거래한 계좌는 건너뛴다.
    late_discovery  BOOLEAN        NOT NULL DEFAULT FALSE,
    applied_at      DATETIME(6)    NULL,
    created_at      DATETIME(6)    NULL,
    updated_at      DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invest_stock_splits UNIQUE (symbol, ex_date),
    INDEX idx_invest_stock_splits_pending (applied_at, ex_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- 계좌별 조정 기록. 거래 내역(invest_trades)은 손대지 않고 여기에 따로 남긴다.
CREATE TABLE invest_split_adjustments (
    id             UUID           NOT NULL,
    split_id       UUID           NOT NULL,
    account_id     UUID           NOT NULL,
    symbol         VARCHAR(20)    NOT NULL,
    new_rate       DECIMAL(18, 6) NOT NULL,
    old_rate       DECIMAL(18, 6) NOT NULL,
    qty_before     BIGINT         NOT NULL,
    qty_after      BIGINT         NOT NULL,
    avg_cost_before DECIMAL(18, 4) NOT NULL,
    avg_cost_after  DECIMAL(18, 4) NOT NULL,
    -- 병합으로 생긴 소수 주를 분할 후 시세로 현금 정산한 금액
    cash_in_lieu   DECIMAL(18, 2) NOT NULL DEFAULT 0,
    -- 늦게 알게 된 분할이라 이 계좌는 조정하지 않은 경우
    skipped        BOOLEAN        NOT NULL DEFAULT FALSE,
    week_end_date  DATE           NULL,
    created_at     DATETIME(6)    NULL,
    updated_at     DATETIME(6)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invest_split_adjustments_account
        FOREIGN KEY (account_id) REFERENCES invest_accounts (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_invest_split_adjustments UNIQUE (split_id, account_id),
    INDEX idx_invest_split_adjustments_account (account_id, created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
