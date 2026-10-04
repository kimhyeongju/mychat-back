package com.khj.playground.chat.repository;

import com.khj.playground.chat.entity.ChatParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatParticipantRepository
  extends JpaRepository<ChatParticipant, UUID> {
  Optional<ChatParticipant> findByRoomIdAndUserId(UUID roomId, UUID userId);

  boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

  /** 내 DM 목록. 최근 대화 순으로 정렬한다. */
  @Query(
    "SELECT p FROM ChatParticipant p JOIN FETCH p.room r " +
    "WHERE p.userId = :userId " +
    "ORDER BY r.lastMessageAt DESC NULLS LAST"
  )
  List<ChatParticipant> findMyRooms(@Param("userId") UUID userId);

  /** 같은 방의 상대방. DM은 2인이므로 하나만 나온다. */
  List<ChatParticipant> findByRoomIdAndUserIdNot(UUID roomId, UUID userId);
}
