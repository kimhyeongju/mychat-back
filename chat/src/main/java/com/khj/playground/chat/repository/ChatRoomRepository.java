package com.khj.playground.chat.repository;

import com.khj.playground.chat.entity.ChatRoom;
import com.khj.playground.chat.enums.RoomType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
  List<ChatRoom> findByTypeOrderByDisplayOrderAsc(RoomType type);

  Optional<ChatRoom> findByDmKey(String dmKey);
}
