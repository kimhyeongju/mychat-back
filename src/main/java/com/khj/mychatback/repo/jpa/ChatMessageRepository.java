package com.khj.mychatback.repo.jpa;

import com.khj.mychatback.entity.jpa.ChatMessage;
import com.khj.mychatback.entity.jpa.ChatRoom;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository
  extends JpaRepository<ChatMessage, UUID> {
  Slice<ChatMessage> findByChatRoomOrderByIdDesc(
    ChatRoom chatRoom,
    Pageable pageable
  );

  @Modifying
  @Query("DELETE FROM ChatMessage m WHERE m.createdAt < :cutoff")
  int deleteAllCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
