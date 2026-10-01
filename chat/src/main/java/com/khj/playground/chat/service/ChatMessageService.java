package com.khj.playground.chat.service;

import com.khj.playground.chat.dto.ChatMessagePage;
import com.khj.playground.chat.dto.ChatMessageResponse;
import com.khj.playground.chat.entity.ChatMessage;
import com.khj.playground.chat.entity.ChatRoom;
import com.khj.playground.chat.repository.ChatMessageRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

  private static final int PAGE_SIZE = 50;
  /** 발신자당 10초에 보낼 수 있는 메시지 수 */
  private static final int RATE_LIMIT = 10;
  private static final Duration RATE_WINDOW = Duration.ofSeconds(10);

  private final ChatMessageRepository chatMessageRepository;
  private final ChatRoomService chatRoomService;
  private final StringRedisTemplate redisTemplate;

  /**
   * @param cursor null이면 최신 메시지부터, 값이 있으면 그보다 과거 메시지를 반환한다.
   */
  public ChatMessagePage getMessages(UUID roomId, UUID cursor) {
    // 조회 한 건 더 가져와서 다음 페이지 존재 여부를 판단한다.
    var pageable = PageRequest.of(0, PAGE_SIZE + 1);

    List<ChatMessage> found = (cursor == null)
      ? chatMessageRepository.findByRoomIdOrderByIdDesc(roomId, pageable)
      : chatMessageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(
        roomId,
        cursor,
        pageable
      );

    boolean hasMore = found.size() > PAGE_SIZE;
    List<ChatMessage> page = hasMore ? found.subList(0, PAGE_SIZE) : found;

    // DB는 최신순으로 가져왔지만 화면에는 오래된 순으로 그린다.
    List<ChatMessageResponse> content = new ArrayList<>(
      page.stream().map(ChatMessageResponse::from).toList()
    );
    java.util.Collections.reverse(content);

    UUID nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getId();

    return new ChatMessagePage(content, hasMore ? nextCursor : null, hasMore);
  }

  @Transactional
  public ChatMessageResponse send(
    UUID roomId,
    String content,
    ChatSenderResolver.Sender sender
  ) {
    checkRateLimit(sender.id());

    ChatRoom room = chatRoomService.findRoom(roomId);
    ChatMessage message = ChatMessage.create(
      room,
      sender.id(),
      sender.type(),
      sender.nickname(),
      content
    );
    chatMessageRepository.save(message);

    // 사이드바 정렬에 쓰인다.
    room.touch(message.getCreatedAt());

    return ChatMessageResponse.from(message);
  }

  /**
   * 메시지 플러딩을 막는다.
   * 익명 사용자는 세션을 새로 발급받아 우회할 수 있으므로,
   * IP 기준 제한은 Phase C에서 WebSocket 레벨에 추가한다.
   */
  private void checkRateLimit(UUID senderId) {
    String key = "chat:rate:" + senderId;
    Long count = redisTemplate.opsForValue().increment(key);

    if (count != null && count == 1L) {
      redisTemplate.expire(key, RATE_WINDOW);
    }

    if (count != null && count > RATE_LIMIT) {
      throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        "메시지를 너무 빠르게 보내고 있습니다."
      );
    }
  }
}
