package com.khj.playground.chat.repository;

import com.khj.playground.chat.entity.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository
  extends JpaRepository<ChatMessage, UUID> {
  /** 최초 진입 시 최근 메시지 N개 */
  List<ChatMessage> findByRoomIdOrderByIdDesc(UUID roomId, Pageable pageable);

  /**
   * 위로 스크롤할 때 커서보다 이전 메시지 N개.
   * UUID v7은 생성 시각 순으로 정렬되므로 id 비교가 곧 시간 비교가 된다.
   */
  List<ChatMessage> findByRoomIdAndIdLessThanOrderByIdDesc(
    UUID roomId,
    UUID cursor,
    Pageable pageable
  );
}
