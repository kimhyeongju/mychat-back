package com.khj.playground.chat.dto;

import com.khj.playground.chat.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
  UUID id,
  UUID roomId,
  UUID senderId,
  String senderType,
  String senderNickname,
  String content,
  LocalDateTime createdAt
) {
  public static ChatMessageResponse from(ChatMessage message) {
    return new ChatMessageResponse(
      message.getId(),
      message.getRoom().getId(),
      message.getSenderId(),
      message.getSenderType().name(),
      message.getSenderNickname(),
      message.getContent(),
      message.getCreatedAt()
    );
  }
}
