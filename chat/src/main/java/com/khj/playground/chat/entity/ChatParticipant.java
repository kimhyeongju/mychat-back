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
import java.time.LocalDateTime;
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

  /**
   * 사용자가 목록에서 숨긴 시각.
   * 이 시각 이후의 메시지만 조회되며, 새 메시지가 오면 목록에 다시 나타난다.
   * 물리 삭제가 아니므로 상대방의 대화 기록에는 영향이 없다.
   */
  @Column(name = "hidden_at")
  private LocalDateTime hiddenAt;

  public void hide(LocalDateTime at) {
    this.hiddenAt = at;
  }

  /** 숨긴 이후 새 메시지가 왔으면 목록에 다시 보여준다. */
  public boolean shouldShow(LocalDateTime lastMessageAt) {
    if (hiddenAt == null) return true;
    return lastMessageAt != null && lastMessageAt.isAfter(hiddenAt);
  }

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
