package com.khj.mychatback.api.chat.controller;

import com.khj.mychatback.api.chat.dto.ChatMessageResponse;
import com.khj.mychatback.api.chat.dto.CreateLocationRoomRequest;
import com.khj.mychatback.api.chat.dto.JoinRoomResponse;
import com.khj.mychatback.api.chat.dto.RoomSummaryResponse;
import com.khj.mychatback.api.chat.service.ChatMessageService;
import com.khj.mychatback.api.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증(JWT)이 있으면 회원으로, 없으면 익명으로 입장 처리한다.
 * SecurityConfig에서 /api/chat/** 는 인증을 요구하지 않으므로(permitAll이 아니라 anyRequest().authenticated() 대상에서 제외해야 함),
 * 익명 접근을 허용하려면 SecurityConfig의 PUBLIC_ENDPOINTS에 "/api/chat/rooms/nearby", "/api/chat/rooms/location", "/api/chat/rooms/{roomId}/**" 등을 추가해야 한다.
 * DIRECT 관련 엔드포인트(/api/chat/rooms/direct/**)는 인증이 반드시 필요하다.
 */
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

  private final ChatRoomService chatRoomService;
  private final ChatMessageService chatMessageService;

  @GetMapping("/nearby")
  public List<RoomSummaryResponse> findNearbyRooms(
    @RequestParam Double latitude,
    @RequestParam Double longitude
  ) {
    return chatRoomService.findNearbyActiveRooms(latitude, longitude);
  }

  @PostMapping("/location")
  public JoinRoomResponse createLocationRoom(
    @Valid @RequestBody CreateLocationRoomRequest request,
    Authentication authentication
  ) {
    return chatRoomService.createLocationRoomAndJoin(
      request,
      usernameOf(authentication)
    );
  }

  @PostMapping("/{roomId}/join")
  public JoinRoomResponse joinRoom(
    @PathVariable Long roomId,
    Authentication authentication
  ) {
    return chatRoomService.joinRoom(roomId, usernameOf(authentication));
  }

  @PostMapping("/direct/{targetUserId}")
  public JoinRoomResponse getOrCreateDirectRoom(
    @PathVariable Long targetUserId,
    Authentication authentication
  ) {
    return chatRoomService.getOrCreateDirectRoomAndJoin(
      authentication.getName(),
      targetUserId
    );
  }

  @GetMapping("/{roomId}/messages")
  public Slice<ChatMessageResponse> getMessages(
    @PathVariable Long roomId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "30") int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    return chatMessageService.getMessages(roomId, pageable);
  }

  /** 인증이 없는(익명) 요청이면 null을 반환해 서비스 레이어가 익명 참여로 처리하게 한다. */
  private String usernameOf(Authentication authentication) {
    return authentication != null ? authentication.getName() : null;
  }
}
