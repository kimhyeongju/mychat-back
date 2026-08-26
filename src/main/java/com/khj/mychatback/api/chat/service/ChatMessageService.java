package com.khj.mychatback.api.chat.service;

import com.khj.mychatback.api.chat.dto.ChatMessageResponse;
import com.khj.mychatback.api.chat.dto.ChatMessageSendRequest;
import com.khj.mychatback.entity.jpa.ChatMessage;
import com.khj.mychatback.entity.jpa.ChatRoom;
import com.khj.mychatback.entity.jpa.ChatRoomMember;
import com.khj.mychatback.repo.jpa.ChatMessageRepository;
import com.khj.mychatback.repo.jpa.ChatRoomMemberRepository;
import com.khj.mychatback.repo.jpa.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final SimpMessagingTemplate messagingTemplate;

  /** STOMP로 들어온 메시지를 저장하고, 같은 방을 구독 중인 모두에게 브로드캐스트한다. */
  @Transactional
  public void sendMessage(Long roomId, ChatMessageSendRequest request) {
    ChatRoom room = chatRoomRepository
      .findById(roomId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 방입니다."
        )
      );

    ChatRoomMember sender = chatRoomMemberRepository
      .findById(request.memberId())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "방 참여자가 아닙니다."
        )
      );

    if (!sender.getChatRoom().getId().equals(roomId)) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "해당 방의 참여자가 아닙니다."
      );
    }

    ChatMessage message = ChatMessage
      .builder()
      .chatRoom(room)
      .sender(sender)
      .content(request.content())
      .build();
    chatMessageRepository.save(message);

    room.touchActivity();

    messagingTemplate.convertAndSend(
      "/topic/rooms/" + roomId,
      ChatMessageResponse.from(message)
    );
  }

  public Slice<ChatMessageResponse> getMessages(
    Long roomId,
    Pageable pageable
  ) {
    ChatRoom room = chatRoomRepository
      .findById(roomId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 방입니다."
        )
      );

    return chatMessageRepository
      .findByChatRoomOrderByIdDesc(room, pageable)
      .map(ChatMessageResponse::from);
  }
}
