package com.khj.mychatback.api.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DirectRoomSummaryResponse(
  UUID roomId,
  UUID partnerUserId,
  String partnerNickname,
  LocalDateTime lastActivityAt
) {}
