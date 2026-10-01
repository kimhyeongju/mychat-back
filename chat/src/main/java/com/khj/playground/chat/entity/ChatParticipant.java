package com.khj.playground.chat.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** DM 방의 참여자. 로그인 회원만 참여할 수 있다. */
@Getter
@Entity
@Table(
  name = "chat_participants",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_chat_participants_room_user",
      columnNames = { "room_id", "user_id" }
    ),
  }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatParticipant extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private ChatRoom room;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  /** 이 값보다 id가 큰 메시지가 안 읽은 메시지다. */
  @Column(name = "last_read_message_id")
  private UUID lastReadMessageId;

  public static ChatParticipant create(ChatRoom room, UUID userId) {
    ChatParticipant participant = new ChatParticipant();
    participant.room = room;
    participant.userId = userId;
    return participant;
  }

  public void markRead(UUID messageId) {
    // 뒤로 되돌아가지 않도록 더 큰 값일 때만 갱신한다.
    if (
      lastReadMessageId == null || messageId.compareTo(lastReadMessageId) > 0
    ) {
      this.lastReadMessageId = messageId;
    }
  }
}
