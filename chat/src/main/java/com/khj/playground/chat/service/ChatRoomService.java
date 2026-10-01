package com.khj.playground.chat.service;

import com.khj.playground.chat.dto.ChatRoomResponse;
import com.khj.playground.chat.entity.ChatRoom;
import com.khj.playground.chat.enums.RoomType;
import com.khj.playground.chat.repository.ChatRoomRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

  private final ChatRoomRepository chatRoomRepository;

  public List<ChatRoomResponse> listOpenRooms() {
    return chatRoomRepository
      .findByTypeOrderByDisplayOrderAsc(RoomType.OPEN)
      .stream()
      .map(ChatRoomResponse::from)
      .toList();
  }

  /** 오픈 채팅방만 공개 조회를 허용한다. DM은 참여자 확인이 필요하므로 별도 경로로 다룬다. */
  public ChatRoomResponse getOpenRoom(UUID roomId) {
    ChatRoom room = findRoom(roomId);
    if (!room.isOpen()) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "접근할 수 없는 방입니다."
      );
    }
    return ChatRoomResponse.from(room);
  }

  public ChatRoom findRoom(UUID roomId) {
    return chatRoomRepository
      .findById(roomId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 방입니다."
        )
      );
  }
}
