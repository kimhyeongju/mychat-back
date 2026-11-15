-- 한글 종목명 검색용 별칭. 시세 제공자가 영문 이름만 줘서 직접 매핑한다.
-- 한 종목에 별칭이 여러 개일 수 있다(예: GOOGL - 구글, 알파벳).
CREATE TABLE invest_symbol_aliases (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    symbol   VARCHAR(20)  NOT NULL,
    alias    VARCHAR(100) NOT NULL,
    name_en  VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invest_symbol_aliases (symbol, alias)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
