-- 한 번이라도 조회된 종목의 이름을 보관한다. 시세는 Redis 에만 두고 여기에는 넣지 않는다.
CREATE TABLE invest_symbols (
    symbol      VARCHAR(20)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    exchange    VARCHAR(50)  NULL,
    created_at  DATETIME(6)  NULL,
    updated_at  DATETIME(6)  NULL,
    PRIMARY KEY (symbol)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
