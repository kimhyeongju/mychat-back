package com.khj.playground.chat.entity;

import com.khj.playground.chat.enums.RoomType;
import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(
  name = "chat_rooms",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_chat_rooms_dm_key", columnNames = "dm_key"),
  }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoomType type;

  /** OPEN 방의 표시 이름. DIRECT는 상대방 닉네임을 쓰므로 null. */
  @Column(length = 50)
  private String name;

  @Column(length = 200)
  private String description;

  /** DIRECT 전용. 두 회원 ID를 사전식으로 정렬해 이어붙인 값. */
  @Column(name = "dm_key", length = 80)
  private String dmKey;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  /** 사이드바 정렬용. 메시지가 들어올 때 갱신한다. */
  @Column(name = "last_message_at")
  private LocalDateTime lastMessageAt;

  public static ChatRoom createDirect(String dmKey) {
    ChatRoom room = new ChatRoom();
    room.type = RoomType.DIRECT;
    room.dmKey = dmKey;
    return room;
  }

  public void touch(LocalDateTime at) {
    this.lastMessageAt = at;
  }

  public boolean isOpen() {
    return this.type == RoomType.OPEN;
  }
}
