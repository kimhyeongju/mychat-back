package com.khj.playground.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartDirectRequest(@NotNull UUID targetUserId) {}
