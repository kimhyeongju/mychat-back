package com.khj.playground.chat.dto;

import java.util.UUID;

/** 메시지가 삭제됐음을 같은 방의 접속자에게 알린다. 화면에서 해당 메시지를 지우는 데 쓴다. */
public record MessageDeletedEvent(UUID messageId, UUID roomId) {}
