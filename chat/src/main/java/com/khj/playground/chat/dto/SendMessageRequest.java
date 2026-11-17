package com.khj.playground.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
  @NotBlank(message = "{chat-dto.message-content.not-blank}")
  @Size(max = 2000, message = "{chat-dto.message-content.size}")
  String content
) {}
