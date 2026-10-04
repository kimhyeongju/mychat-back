package com.khj.playground.chat.dto;

import com.khj.playground.chat.entity.ChatRoom;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomResponse(
  UUID id,
  String type,
  String name,
  String description,
  LocalDateTime lastMessageAt
) {
  public static ChatRoomResponse from(ChatRoom room) {
    return new ChatRoomResponse(
      room.getId(),
      room.getType().name(),
      room.getName(),
      room.getDescription(),
      room.getLastMessageAt()
    );
  }
}
