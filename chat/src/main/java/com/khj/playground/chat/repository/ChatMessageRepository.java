package com.khj.playground.chat.repository;

import com.khj.playground.chat.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository
  extends JpaRepository<ChatMessage, UUID> {
  /** cursor보다 뒤에 온 메시지 중 내가 보내지 않은 것의 개수 */
  @Query(
    "SELECT COUNT(m) FROM ChatMessage m " +
    "WHERE m.room.id = :roomId AND m.senderId <> :userId " +
    "AND (:lastReadId IS NULL OR m.id > :lastReadId)"
  )
  long countUnread(
    @Param("roomId") UUID roomId,
    @Param("userId") UUID userId,
    @Param("lastReadId") UUID lastReadId
  );

  Optional<ChatMessage> findFirstByRoomIdOrderByIdDesc(UUID roomId);

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
