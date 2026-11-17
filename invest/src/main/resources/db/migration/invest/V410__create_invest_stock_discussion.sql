-- 종목 토론: 글, 댓글, 좋아요. 사용자(user_id)는 다른 모듈 소유라 FK 를 걸지 않고,
-- 탈퇴 시 InvestUserCleanup 이 지운다. 글을 지우면 댓글·좋아요는 FK 로 함께 지워진다.
CREATE TABLE invest_stock_posts (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    symbol         VARCHAR(20)    NOT NULL,
    user_id        UUID           NOT NULL,
    content        VARCHAR(1000)  NOT NULL,
    like_count     INT            NOT NULL DEFAULT 0,
    comment_count  INT            NOT NULL DEFAULT 0,
    created_at     DATETIME(6)    NOT NULL,
    updated_at     DATETIME(6)    NULL,
    PRIMARY KEY (id),
    INDEX idx_stock_posts_symbol_created (symbol, created_at, id),
    INDEX idx_stock_posts_symbol_likes (symbol, like_count, created_at),
    INDEX idx_stock_posts_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

CREATE TABLE invest_stock_comments (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    post_id     BIGINT        NOT NULL,
    user_id     UUID          NOT NULL,
    content     VARCHAR(500)  NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_stock_comments_post (post_id, id),
    INDEX idx_stock_comments_user (user_id),
    CONSTRAINT fk_stock_comments_post FOREIGN KEY (post_id)
        REFERENCES invest_stock_posts (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

CREATE TABLE invest_stock_post_likes (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    post_id     BIGINT       NOT NULL,
    user_id     UUID         NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_post_likes UNIQUE (post_id, user_id),
    INDEX idx_stock_post_likes_user (user_id),
    CONSTRAINT fk_stock_post_likes_post FOREIGN KEY (post_id)
        REFERENCES invest_stock_posts (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;
