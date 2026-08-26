package com.khj.mychatback.entity.jpa;

import com.khj.mychatback.enums.RoomType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 엔티티.
 * - LOCATION: centerLatitude/centerLongitude/radiusMeters 필수. 방을 만든 사용자의 위치가 중심점으로 고정된다.
 * - DIRECT: dmKey로 두 회원 조합을 고유하게 식별 (같은 상대와 재대화 시 기존 방 재사용).
 * - PUBLIC: 향후 자유 생성 기능용. 현재는 title만 사용.
 */
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoomType type;

  /** PUBLIC 방 제목 등. LOCATION/DIRECT는 null 가능 (프론트에서 별도 표시명 구성) */
  @Column(length = 50)
  private String title;

  /** LOCATION 타입 전용: 방 생성 시점의 생성자 위치 */
  private Double centerLatitude;
  private Double centerLongitude;

  /** LOCATION 타입 전용: 반경(미터). 예: 100, 1000 */
  private Integer radiusMeters;

  /**
   * DIRECT 타입 전용: 두 회원 ID를 정렬해 조합한 문자열 (예: "3_17").
   * 같은 두 회원 사이의 방을 유일하게 식별하기 위한 키.
   */
  @Column(name = "dm_key", length = 50)
  private String dmKey;

  /** 마지막 메시지 시각. "활성화된 방" 판단 및 반경 검색 결과 정렬에 사용 */
  @Column(name = "last_activity_at")
  private LocalDateTime lastActivityAt;

  @Builder
  private ChatRoom(
    RoomType type,
    String title,
    Double centerLatitude,
    Double centerLongitude,
    Integer radiusMeters,
    String dmKey
  ) {
    this.type = type;
    this.title = title;
    this.centerLatitude = centerLatitude;
    this.centerLongitude = centerLongitude;
    this.radiusMeters = radiusMeters;
    this.dmKey = dmKey;
    this.lastActivityAt = LocalDateTime.now();
  }

  public static ChatRoom createLocationRoom(
    double latitude,
    double longitude,
    int radiusMeters
  ) {
    return ChatRoom
      .builder()
      .type(RoomType.LOCATION)
      .centerLatitude(latitude)
      .centerLongitude(longitude)
      .radiusMeters(radiusMeters)
      .build();
  }

  public static ChatRoom createDirectRoom(String dmKey) {
    return ChatRoom.builder().type(RoomType.DIRECT).dmKey(dmKey).build();
  }

  public void touchActivity() {
    this.lastActivityAt = LocalDateTime.now();
  }
}
