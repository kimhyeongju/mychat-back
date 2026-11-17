package com.khj.playground.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartDirectRequest(
  @NotNull(message = "{chat-dto.target-user-id.not-null}") UUID targetUserId
) {}
