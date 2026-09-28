CREATE TABLE board_posts (
    id              UUID          NOT NULL,
    board_id        UUID          NOT NULL,
    author_id       UUID          NOT NULL,
    author_nickname VARCHAR(12)   NOT NULL,
    title           VARCHAR(200)  NOT NULL,
    content         MEDIUMTEXT    NOT NULL,
    view_count      BIGINT        NOT NULL DEFAULT 0,
    comment_count   INT           NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NULL,
    updated_at      DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_board_posts_board
        FOREIGN KEY (board_id) REFERENCES board_boards (id),
    -- 목록 조회: 게시판별 최신순
    INDEX idx_board_posts_board_created (board_id, created_at DESC),
    -- 내가 쓴 글 조회
    INDEX idx_board_posts_author (author_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;