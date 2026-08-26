package com.khj.mychatback.entity.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지.
 * 발신자는 회원/익명 여부와 관계없이 ChatRoomMember로 통일해서 참조한다.
 * 생성 후 7일이 지나면 스케줄러(추후 구현)가 자동 삭제한다 — createdAt 기준으로 판단.
 */
@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_member_id", nullable = false)
  private ChatRoomMember sender;

  @Column(nullable = false, length = 1000)
  private String content;

  @Builder
  private ChatMessage(
    ChatRoom chatRoom,
    ChatRoomMember sender,
    String content
  ) {
    this.chatRoom = chatRoom;
    this.sender = sender;
    this.content = content;
  }
}
