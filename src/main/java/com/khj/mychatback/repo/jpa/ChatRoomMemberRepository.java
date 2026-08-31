package com.khj.mychatback.repo.jpa;

import com.khj.mychatback.entity.jpa.ChatRoom;
import com.khj.mychatback.entity.jpa.ChatRoomMember;
import com.khj.mychatback.entity.jpa.User;
import com.khj.mychatback.enums.RoomType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository
  extends JpaRepository<ChatRoomMember, UUID> {
  List<ChatRoomMember> findByChatRoomAndLeftAtIsNull(ChatRoom chatRoom);

  Optional<ChatRoomMember> findByChatRoomAndUser(ChatRoom chatRoom, User user);

  Optional<ChatRoomMember> findByChatRoomAndAnonymousId(
    ChatRoom chatRoom,
    String anonymousId
  );

  boolean existsByChatRoomAndNickname(ChatRoom chatRoom, String nickname);

  List<ChatRoomMember> findByUserAndChatRoom_TypeOrderByChatRoom_LastActivityAtDesc(
    User user,
    RoomType type
  );

  Optional<ChatRoomMember> findByChatRoomAndUserNot(
    ChatRoom chatRoom,
    User user
  );
}
