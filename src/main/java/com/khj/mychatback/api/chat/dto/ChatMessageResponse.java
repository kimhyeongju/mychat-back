package com.khj.mychatback.api.chat.dto;

import com.khj.mychatback.entity.jpa.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
  Long messageId,
  Long roomId,
  Long senderMemberId,
  String senderNickname,
  boolean senderAnonymous,
  String content,
  LocalDateTime createdAt
) {
  public static ChatMessageResponse from(ChatMessage message) {
    return new ChatMessageResponse(
      message.getId(),
      message.getChatRoom().getId(),
      message.getSender().getId(),
      message.getSender().getNickname(),
      message.getSender().isAnonymous(),
      message.getContent(),
      message.getCreatedAt()
    );
  }
}
