ALTER TABLE chat_participants
    ADD COLUMN hidden_at DATETIME(6) NULL AFTER last_read_message_id;