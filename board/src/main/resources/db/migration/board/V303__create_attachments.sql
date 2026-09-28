CREATE TABLE board_attachments (
    id             UUID          NOT NULL,
    post_id        UUID          NOT NULL,
    original_name  VARCHAR(255)  NOT NULL,
    stored_name    VARCHAR(100)  NOT NULL,
    content_type   VARCHAR(100)  NOT NULL,
    size_bytes     BIGINT        NOT NULL,
    created_at     DATETIME(6)   NULL,
    updated_at     DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_board_attachments_post
        FOREIGN KEY (post_id) REFERENCES board_posts (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_board_attachments_stored_name UNIQUE (stored_name),
    INDEX idx_board_attachments_post (post_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;