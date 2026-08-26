package com.khj.mychatback.api.chat.dto;

import com.khj.mychatback.entity.jpa.ChatRoomMember;

public record JoinRoomResponse(Long roomId, Long memberId, String nickname) {
  public static JoinRoomResponse from(ChatRoomMember member) {
    return new JoinRoomResponse(
      member.getChatRoom().getId(),
      member.getId(),
      member.getNickname()
    );
  }
}
