CREATE TABLE chat_rooms (
    id              UUID         NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    name            VARCHAR(50)  NULL,
    description     VARCHAR(200) NULL,
    -- DM 전용. 두 회원 ID를 정렬해 이어붙인 값으로 같은 방을 다시 찾는다.
    dm_key          VARCHAR(80)  NULL,
    display_order   INT          NOT NULL DEFAULT 0,
    last_message_at DATETIME(6)  NULL,
    created_at      DATETIME(6)  NULL,
    updated_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_rooms_dm_key UNIQUE (dm_key),
    INDEX idx_chat_rooms_type_order (type, display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- 오픈 채팅방은 마이그레이션으로 관리한다.
-- 사용자가 방을 만들 수 있게 하면 스팸 방지 로직이 필요해지므로 1차에서는 고정한다.
INSERT INTO chat_rooms
    (id, type, name, description, display_order, created_at, updated_at)
VALUES
    ('01900000-0000-7000-8000-000000000011', 'OPEN', '아무말 대잔치',
     '주제 없이 떠드는 곳', 1, NOW(6), NOW(6)),
    ('01900000-0000-7000-8000-000000000012', 'OPEN', '개발 이야기',
     '코드와 삽질에 대하여', 2, NOW(6), NOW(6));