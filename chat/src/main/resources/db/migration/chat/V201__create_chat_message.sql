CREATE TABLE chat_anonymous_sessions (
    id         UUID         NOT NULL,
    nickname   VARCHAR(30)  NOT NULL,
    created_at DATETIME(6)  NULL,
    updated_at DATETIME(6)  NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

CREATE TABLE chat_messages (
    id              UUID         NOT NULL,
    room_id         UUID         NOT NULL,
    -- 로그인 사용자면 users.id, 익명이면 chat_anonymous_sessions.id
    sender_id       UUID         NOT NULL,
    sender_type     VARCHAR(20)  NOT NULL,
    sender_nickname VARCHAR(30)  NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    created_at      DATETIME(6)  NULL,
    updated_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_room
        FOREIGN KEY (room_id) REFERENCES chat_rooms (id)
        ON DELETE CASCADE,
    -- 방별 최신 메시지 조회 및 커서 페이징
    INDEX idx_chat_messages_room_id (room_id, id DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;