package com.khj.playground.chat.controller;

import com.khj.playground.chat.config.StompPrincipal;
import com.khj.playground.chat.dto.ChatMessageResponse;
import com.khj.playground.chat.dto.SendMessageRequest;
import com.khj.playground.chat.enums.SenderType;
import com.khj.playground.chat.service.ChatMessageService;
import com.khj.playground.chat.service.ChatRoomService;
import com.khj.playground.chat.service.ChatSenderResolver;
import com.khj.playground.chat.service.DirectChatService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

  private final ChatMessageService chatMessageService;
  private final ChatRoomService chatRoomService;
  private final SimpMessagingTemplate messagingTemplate;
  private final DirectChatService directChatService;

  /**
   * 클라이언트가 /app/chat/rooms/{roomId}/send 로 보낸다.
   *
   * principal은 CONNECT 시점에 확정된 값이다. 클라이언트가 페이로드에
   * 발신자 정보를 담아 보내더라도 사용하지 않는다. 그렇게 하면 신원 위조가 가능해진다.
   */
  @MessageMapping("/chat/rooms/{roomId}/send")
  public void send(
    @DestinationVariable("roomId") UUID roomId,
    @Valid @Payload SendMessageRequest request,
    StompPrincipal principal
  ) {
    // 방 접근 권한 확인. DM은 Phase D에서 별도 경로로 다룬다.
    chatRoomService.getOpenRoom(roomId);

    var sender = new ChatSenderResolver.Sender(
      principal.id(),
      principal.type(),
      principal.nickname()
    );

    ChatMessageResponse saved = chatMessageService.send(
      roomId,
      request.content(),
      sender
    );

    messagingTemplate.convertAndSend("/topic/rooms/" + roomId, saved);
  }

  /**
   * DM 전송. 익명 사용자는 참여자가 될 수 없으므로 자동으로 차단된다.
   * 구독 경로가 /topic이 아니라 /user/queue이므로 제3자가 엿들을 수 없다.
   */
  @MessageMapping("/chat/dm/{roomId}/send")
  public void sendDirect(
    @DestinationVariable("roomId") UUID roomId,
    @Valid @Payload SendMessageRequest request,
    StompPrincipal principal
  ) {
    if (principal.type() != SenderType.MEMBER) {
      throw new IllegalArgumentException(
        "DM은 로그인 사용자만 이용할 수 있습니다."
      );
    }

    directChatService.requireParticipant(roomId, principal.id());

    var sender = new ChatSenderResolver.Sender(
      principal.id(),
      principal.type(),
      principal.nickname()
    );
    ChatMessageResponse saved = chatMessageService.send(
      roomId,
      request.content(),
      sender
    );

    // 참여자 각자의 개인 큐로 보낸다.
    messagingTemplate.convertAndSendToUser(
      principal.getName(),
      "/queue/dm/" + roomId,
      saved
    );
    directChatService
      .findPartnerIds(roomId, principal.id())
      .forEach(partnerId ->
        messagingTemplate.convertAndSendToUser(
          SenderType.MEMBER + ":" + partnerId,
          "/queue/dm/" + roomId,
          saved
        )
      );
  }
}
