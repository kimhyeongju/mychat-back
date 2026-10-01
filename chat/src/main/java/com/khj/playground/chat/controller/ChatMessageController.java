package com.khj.playground.chat.controller;

import com.khj.playground.chat.dto.AnonymousSessionResponse;
import com.khj.playground.chat.dto.ChatMessagePage;
import com.khj.playground.chat.dto.ChatMessageResponse;
import com.khj.playground.chat.dto.SendMessageRequest;
import com.khj.playground.chat.service.ChatMessageService;
import com.khj.playground.chat.service.ChatRoomService;
import com.khj.playground.chat.service.ChatSenderResolver;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

  private final ChatMessageService chatMessageService;
  private final ChatRoomService chatRoomService;
  private final ChatSenderResolver senderResolver;

  /** 익명 신원 발급. 클라이언트가 응답의 anonymousId를 보관한다. */
  @PostMapping("/anonymous-session")
  @ResponseStatus(HttpStatus.CREATED)
  public AnonymousSessionResponse issueAnonymousSession() {
    return AnonymousSessionResponse.from(
      senderResolver.issueAnonymousSession()
    );
  }

  @GetMapping("/rooms/open/{roomId}/messages")
  public ChatMessagePage getOpenRoomMessages(
    @PathVariable("roomId") UUID roomId,
    @RequestParam(name = "cursor", required = false) UUID cursor
  ) {
    // 방 접근 권한을 먼저 확인한다. DM이면 403이 난다.
    chatRoomService.getOpenRoom(roomId);
    return chatMessageService.getMessages(roomId, cursor);
  }

  @PostMapping("/rooms/open/{roomId}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  public ChatMessageResponse sendToOpenRoom(
    @PathVariable("roomId") UUID roomId,
    @Valid @RequestBody SendMessageRequest request,
    @RequestHeader(name = "X-Anonymous-Id", required = false) UUID anonymousId,
    Authentication authentication
  ) {
    chatRoomService.getOpenRoom(roomId);
    var sender = senderResolver.resolve(authentication, anonymousId);
    return chatMessageService.send(roomId, request.content(), sender);
  }
}
