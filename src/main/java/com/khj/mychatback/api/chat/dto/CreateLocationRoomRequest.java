package com.khj.mychatback.api.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateLocationRoomRequest(
  @NotNull Double latitude,
  @NotNull Double longitude,

  /** 미터 단위. 예: 100, 1000 */
  @NotNull @Min(50) @Max(5000) Integer radiusMeters
) {}
