package com.khj.mychatback.repo.jpa;

import com.khj.mychatback.entity.jpa.ChatRoom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
  Optional<ChatRoom> findByDmKey(String dmKey);

  /**
   * 내 위치(latitude, longitude)에서 각 방의 중심점까지의 실제 거리(미터, Haversine 공식)를 계산해
   * 그 거리가 방 자신의 반경(radiusMeters) 이내인 LOCATION 방만 반환한다.
   * activeSince 이후 활동(lastActivityAt)이 있는 방만 대상으로 해서, 방치된 방은 검색에서 제외한다.
   */
  @Query(
    value = """
            SELECT * FROM chat_rooms r
            WHERE r.type = 'LOCATION'
              AND r.last_activity_at >= :activeSince
              AND (
                6371000 * ACOS(
                  COS(RADIANS(:latitude)) * COS(RADIANS(r.center_latitude))
                    * COS(RADIANS(r.center_longitude) - RADIANS(:longitude))
                  + SIN(RADIANS(:latitude)) * SIN(RADIANS(r.center_latitude))
                )
              ) <= r.radius_meters
            ORDER BY r.last_activity_at DESC
            """,
    nativeQuery = true
  )
  List<ChatRoom> findActiveLocationRoomsNear(
    @Param("latitude") double latitude,
    @Param("longitude") double longitude,
    @Param("activeSince") LocalDateTime activeSince
  );
}
