package com.khj.mychatback.api.chat.dto;

import com.khj.mychatback.entity.jpa.ChatRoom;
import com.khj.mychatback.enums.RoomType;
import java.time.LocalDateTime;

public record RoomSummaryResponse(
  Long roomId,
  RoomType type,
  String title,
  Integer radiusMeters,
  LocalDateTime lastActivityAt
) {
  public static RoomSummaryResponse from(ChatRoom room) {
    return new RoomSummaryResponse(
      room.getId(),
      room.getType(),
      room.getTitle(),
      room.getRadiusMeters(),
      room.getLastActivityAt()
    );
  }
}
