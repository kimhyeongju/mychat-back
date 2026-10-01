package com.khj.playground.chat.entity;

import com.khj.playground.chat.enums.SenderType;
import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private ChatRoom room;

  /** MEMBER면 users.id, ANONYMOUS면 chat_anonymous_sessions.id */
  @Column(name = "sender_id", nullable = false)
  private UUID senderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "sender_type", nullable = false, length = 20)
  private SenderType senderType;

  /** 전송 시점 닉네임을 복사해 둔다. 이후 닉네임이 바뀌어도 과거 대화는 그대로 유지된다. */
  @Column(name = "sender_nickname", nullable = false, length = 30)
  private String senderNickname;

  @Column(nullable = false, length = 2000)
  private String content;

  public static ChatMessage create(
    ChatRoom room,
    UUID senderId,
    SenderType senderType,
    String senderNickname,
    String content
  ) {
    ChatMessage message = new ChatMessage();
    message.room = room;
    message.senderId = senderId;
    message.senderType = senderType;
    message.senderNickname = senderNickname;
    message.content = content;
    return message;
  }
}
