package com.khj.playground.board.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(
  name = "board_boards",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_board_boards_slug", columnNames = "slug"),
  }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  /** URL에 노출되는 식별자. 예: /boards/free */
  @Column(nullable = false, length = 30)
  private String slug;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(length = 200)
  private String description;

  /** true면 관리자만 글을 쓸 수 있다. 읽기는 누구나 가능. */
  @Column(name = "admin_only", nullable = false)
  private boolean adminOnly;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;
}
