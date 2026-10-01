package com.khj.playground.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** 사이드바에 표시할 DM 방 정보 */
public record DirectRoomResponse(
  UUID roomId,
  UUID partnerId,
  String partnerNickname,
  String lastMessagePreview,
  LocalDateTime lastMessageAt,
  long unreadCount
) {}
