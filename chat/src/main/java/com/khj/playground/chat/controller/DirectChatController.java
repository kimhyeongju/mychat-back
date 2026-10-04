package com.khj.playground.chat.controller;

import com.khj.playground.chat.dto.ChatMessagePage;
import com.khj.playground.chat.dto.ChatMessageResponse;
import com.khj.playground.chat.dto.DirectRoomResponse;
import com.khj.playground.chat.dto.SendMessageRequest;
import com.khj.playground.chat.dto.StartDirectRequest;
import com.khj.playground.chat.service.ChatMessageService;
import com.khj.playground.chat.service.ChatSenderResolver;
import com.khj.playground.chat.service.DirectChatService;
import com.khj.playground.common.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * DM은 로그인 회원 전용이다.
 * ChatPublicEndpoints에 등록하지 않으므로 모든 경로가 자동으로 인증을 요구한다.
 */
@RestController
@RequestMapping("/api/chat/dm")
@RequiredArgsConstructor
public class DirectChatController {

  private final DirectChatService directChatService;
  private final ChatMessageService chatMessageService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping("/rooms")
  public List<DirectRoomResponse> listMyRooms(Authentication authentication) {
    return directChatService.listMyRooms(myId(authentication));
  }

  @PostMapping("/rooms")
  @ResponseStatus(HttpStatus.CREATED)
  public DirectRoomResponse startDirect(
    @Valid @RequestBody StartDirectRequest request,
    Authentication authentication
  ) {
    return directChatService.startDirect(
      myId(authentication),
      request.targetUserId()
    );
  }

  @GetMapping("/rooms/{roomId}/messages")
  public ChatMessagePage getMessages(
    @PathVariable("roomId") UUID roomId,
    @RequestParam(name = "cursor", required = false) UUID cursor,
    Authentication authentication
  ) {
    UUID me = myId(authentication);
    directChatService.requireParticipant(roomId, me);

    // 방을 숨긴 적이 있으면 그 이후 메시지만 보여준다.
    LocalDateTime visibleSince = directChatService.findVisibleSince(roomId, me);
    ChatMessagePage page = chatMessageService.getMessages(
      roomId,
      cursor,
      visibleSince
    );

    if (cursor == null) {
      directChatService.markRead(roomId, me);
    }
    return page;
  }

  @PostMapping("/rooms/{roomId}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  public ChatMessageResponse send(
    @PathVariable("roomId") UUID roomId,
    @Valid @RequestBody SendMessageRequest request,
    Authentication authentication
  ) {
    var user = currentUserProvider.resolve(authentication);
    directChatService.requireParticipant(roomId, user.id());

    var sender = new ChatSenderResolver.Sender(
      user.id(),
      com.khj.playground.chat.enums.SenderType.MEMBER,
      user.nickname()
    );
    return chatMessageService.send(roomId, request.content(), sender);
  }

  @PostMapping("/rooms/{roomId}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markRead(
    @PathVariable("roomId") UUID roomId,
    Authentication authentication
  ) {
    UUID me = myId(authentication);
    directChatService.requireParticipant(roomId, me);
    directChatService.markRead(roomId, me);
  }

  private UUID myId(Authentication authentication) {
    return currentUserProvider.resolve(authentication).id();
  }

  /**
   * 내 목록에서 방을 숨긴다.
   * 실제 삭제가 아니라 숨김이므로, 상대가 다시 메시지를 보내면 목록에 나타난다.
   */
  @DeleteMapping("/rooms/{roomId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void hideRoom(
    @PathVariable("roomId") UUID roomId,
    Authentication authentication
  ) {
    UUID me = myId(authentication);
    directChatService.requireParticipant(roomId, me);
    directChatService.hideRoom(roomId, me);
  }
}
