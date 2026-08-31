package com.khj.mychatback.entity.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 채팅방 참여자.
 * - 회원 참여: user != null, anonymousId == null
 * - 익명 참여: user == null, anonymousId(클라이언트가 세션 단위로 발급한 UUID) 사용
 * - nickname은 참여 시점에 스냅샷으로 저장 (회원이 나중에 닉네임을 바꿔도 과거 채팅 표시는 유지).
 */
@Getter
@Entity
@Table(name = "chat_room_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  /** 회원 참여자인 경우에만 값 존재 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  /** 익명 참여자인 경우에만 값 존재 (WebSocket 세션에서 발급) */
  @Column(name = "anonymous_id", length = 50)
  private String anonymousId;

  @Column(nullable = false, length = 30)
  private String nickname;

  private java.time.LocalDateTime leftAt;

  @Builder
  private ChatRoomMember(
    ChatRoom chatRoom,
    User user,
    String anonymousId,
    String nickname
  ) {
    this.chatRoom = chatRoom;
    this.user = user;
    this.anonymousId = anonymousId;
    this.nickname = nickname;
  }

  public static ChatRoomMember ofMember(ChatRoom chatRoom, User user) {
    return ChatRoomMember
      .builder()
      .chatRoom(chatRoom)
      .user(user)
      .nickname(user.getNickname())
      .build();
  }

  public static ChatRoomMember ofAnonymous(
    ChatRoom chatRoom,
    String anonymousId,
    String nickname
  ) {
    return ChatRoomMember
      .builder()
      .chatRoom(chatRoom)
      .anonymousId(anonymousId)
      .nickname(nickname)
      .build();
  }

  public boolean isAnonymous() {
    return user == null;
  }

  public void leave() {
    this.leftAt = java.time.LocalDateTime.now();
  }
}
