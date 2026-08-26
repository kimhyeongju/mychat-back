package com.khj.mychatback.repo.jpa;

import com.khj.mychatback.entity.jpa.ChatRoom;
import com.khj.mychatback.entity.jpa.ChatRoomMember;
import com.khj.mychatback.entity.jpa.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository
  extends JpaRepository<ChatRoomMember, Long> {
  List<ChatRoomMember> findByChatRoomAndLeftAtIsNull(ChatRoom chatRoom);

  Optional<ChatRoomMember> findByChatRoomAndUser(ChatRoom chatRoom, User user);

  Optional<ChatRoomMember> findByChatRoomAndAnonymousId(
    ChatRoom chatRoom,
    String anonymousId
  );

  boolean existsByChatRoomAndNickname(ChatRoom chatRoom, String nickname);
}
