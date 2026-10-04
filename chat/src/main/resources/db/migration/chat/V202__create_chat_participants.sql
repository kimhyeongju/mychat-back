CREATE TABLE chat_participants (
    id                   UUID        NOT NULL,
    room_id              UUID        NOT NULL,
    user_id              UUID        NOT NULL,
    -- 이 값보다 id가 큰 메시지가 읽지 않은 메시지다.
    last_read_message_id UUID        NULL,
    created_at           DATETIME(6) NULL,
    updated_at           DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_participants_room
        FOREIGN KEY (room_id) REFERENCES chat_rooms (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_chat_participants_room_user UNIQUE (room_id, user_id),
    -- 내 DM 목록 조회
    INDEX idx_chat_participants_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;