package com.khj.playground.chat.controller;

import com.khj.playground.chat.dto.AnonymousSessionResponse;
import com.khj.playground.chat.dto.ChatMessagePage;
import com.khj.playground.chat.dto.ChatMessageResponse;
import com.khj.playground.chat.dto.MessageDeletedEvent;
import com.khj.playground.chat.dto.SendMessageRequest;
import com.khj.playground.chat.service.ChatMessageService;
import com.khj.playground.chat.service.ChatRoomService;
import com.khj.playground.chat.service.ChatSenderResolver;
import com.khj.playground.chat.service.DirectChatService;
import com.khj.playground.chat.enums.SenderType;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  private final CurrentUserProvider currentUserProvider;
  private final DirectChatService directChatService;
  private final SimpMessagingTemplate messagingTemplate;

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

  /**
   * 메시지 삭제. 오픈 채팅과 DM이 같은 경로를 쓴다.
   * 본인이 보낸 메시지만 지울 수 있고, 관리자는 모든 메시지를 지울 수 있다.
   * 익명 사용자도 자신의 메시지를 지울 수 있도록 공개 경로로 두고 여기서 직접 권한을 확인한다.
   */
  @DeleteMapping("/messages/{messageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteMessage(
    @PathVariable("messageId") UUID messageId,
    @RequestHeader(name = "X-Anonymous-Id", required = false) UUID anonymousId,
    Authentication authentication
  ) {
    UUID requesterId;
    boolean admin = false;

    if (authentication != null && authentication.isAuthenticated()) {
      AuthenticatedUser user = currentUserProvider.resolve(authentication);
      requesterId = user.id();
      admin = user.admin();
    } else {
      // 토큰이 없으면 익명 세션으로 신원을 확인한다. 세션이 없으면 401이 난다.
      requesterId = senderResolver.resolve(null, anonymousId).id();
    }

    var deleted = chatMessageService.delete(messageId, requesterId, admin);
    var event = new MessageDeletedEvent(deleted.messageId(), deleted.roomId());

    if (deleted.openRoom()) {
      messagingTemplate.convertAndSend(
        "/topic/rooms/" + deleted.roomId() + "/deleted",
        event
      );
    } else {
      // DM은 참여자 각자의 개인 큐로만 알린다.
      directChatService
        .findParticipantIds(deleted.roomId())
        .forEach(userId ->
          messagingTemplate.convertAndSendToUser(
            SenderType.MEMBER + ":" + userId,
            "/queue/dm/" + deleted.roomId() + "/deleted",
            event
          )
        );
    }
  }
}
