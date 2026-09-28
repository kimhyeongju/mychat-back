CREATE TABLE board_comments (
    id              UUID          NOT NULL,
    post_id         UUID          NOT NULL,
    author_id       UUID          NOT NULL,
    author_nickname VARCHAR(12)   NOT NULL,
    content         VARCHAR(1000) NOT NULL,
    created_at      DATETIME(6)   NULL,
    updated_at      DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_board_comments_post
        FOREIGN KEY (post_id) REFERENCES board_posts (id)
        ON DELETE CASCADE,
    -- 글 상세에서 댓글을 오래된 순으로 읽는다
    INDEX idx_board_comments_post_created (post_id, created_at),
    INDEX idx_board_comments_author (author_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;