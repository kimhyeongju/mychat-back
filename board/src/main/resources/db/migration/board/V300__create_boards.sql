CREATE TABLE board_boards (
    id            UUID         NOT NULL,
    slug          VARCHAR(30)  NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    description   VARCHAR(200) NULL,
    admin_only    TINYINT(1)   NOT NULL DEFAULT 0,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NULL,
    updated_at    DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_board_boards_slug UNIQUE (slug)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- 환경마다 같은 id를 갖도록 고정 UUID를 쓴다.
INSERT INTO board_boards
    (id, slug, name, description, admin_only, display_order, created_at, updated_at)
VALUES
    ('01900000-0000-7000-8000-000000000001', 'notice', '공지사항',
     '운영 관련 알림', 1, 1, NOW(6), NOW(6)),
    ('01900000-0000-7000-8000-000000000002', 'free', '자유게시판',
     '아무 이야기나 나눠요', 0, 2, NOW(6), NOW(6));