-- 정산이 끝난 주. week_end_date 가 유니크라 같은 주를 두 번 정산할 수 없다.
CREATE TABLE invest_weeks (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    week_end_date      DATE        NOT NULL,
    participant_count  INT         NOT NULL,
    settled_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invest_weeks_end UNIQUE (week_end_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- 주마다 그 주에 거래한 사용자 전원의 마감 시점 성적. rank_no 1~3 이 포인트를 받는다.
CREATE TABLE invest_weekly_results (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    week_id       BIGINT         NOT NULL,
    user_id       UUID           NOT NULL,
    rank_no       INT            NOT NULL,
    return_rate   DECIMAL(14, 6) NOT NULL,
    final_equity  DECIMAL(18, 2) NOT NULL,
    points        INT            NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invest_weekly_results UNIQUE (week_id, user_id),
    CONSTRAINT fk_invest_weekly_results_week
        FOREIGN KEY (week_id) REFERENCES invest_weeks (id) ON DELETE CASCADE,
    -- 리더보드·프로필 집계
    INDEX idx_invest_weekly_results_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
