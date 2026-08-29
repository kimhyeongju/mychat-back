package com.khj.mychatback.api.chat.dto;

import java.time.LocalDateTime;

public record DirectRoomSummaryResponse(
  Long roomId,
  Long partnerUserId,
  String partnerNickname,
  LocalDateTime lastActivityAt
) {}
