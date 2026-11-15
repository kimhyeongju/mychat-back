-- 거래가 속한 주(주 마감일). 주간 정산 시 그 주에 거래한 사용자를 가려내고, 내역을 주차별로 묶는 데 쓴다.
-- 이 기능 이전의 거래는 NULL 이다.
ALTER TABLE invest_trades
    ADD COLUMN week_end_date DATE NULL AFTER amount,
    ADD INDEX idx_invest_trades_week (week_end_date);
