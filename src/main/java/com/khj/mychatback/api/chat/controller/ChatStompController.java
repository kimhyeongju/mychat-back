package com.khj.mychatback.api.chat.controller;

import com.khj.mychatback.api.chat.dto.ChatMessageSendRequest;
import com.khj.mychatback.api.chat.service.ChatMessageService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 클라이언트가 /app/rooms/{roomId}/send 로 보낸 메시지를 처리한다.
 * 결과는 ChatMessageService가 /topic/rooms/{roomId} 로 직접 브로드캐스트하므로 이 메서드는 반환값이 없다.
 */
@Controller
@RequiredArgsConstructor
public class ChatStompController {

  private final ChatMessageService chatMessageService;

  @MessageMapping("/rooms/{roomId}/send")
  public void sendMessage(
    @DestinationVariable("roomId") UUID roomId,
    @Valid @Payload ChatMessageSendRequest request
  ) {
    chatMessageService.sendMessage(roomId, request);
  }
}
