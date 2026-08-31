package com.khj.mychatback.api.chat.dto;

import com.khj.mychatback.entity.jpa.ChatRoomMember;
import java.util.UUID;

public record JoinRoomResponse(UUID roomId, UUID memberId, String nickname) {
  public static JoinRoomResponse from(ChatRoomMember member) {
    return new JoinRoomResponse(
      member.getChatRoom().getId(),
      member.getId(),
      member.getNickname()
    );
  }
}
