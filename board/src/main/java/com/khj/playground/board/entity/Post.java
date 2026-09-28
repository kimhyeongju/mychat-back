package com.khj.playground.board.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(name = "board_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  /** 같은 모듈 안이므로 연관관계를 맺어도 된다. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  /**
   * auth 모듈의 User를 직접 참조하지 않는다.
   * 닉네임은 작성 시점 값을 복사해두므로, 작성자가 닉네임을 바꿔도 옛 글의 표기는 유지된다.
   */
  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "author_nickname", nullable = false, length = 12)
  private String authorNickname;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
  private String content;

  @Column(name = "view_count", nullable = false)
  private long viewCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  public static Post create(
    Board board,
    UUID authorId,
    String authorNickname,
    String title,
    String content
  ) {
    Post post = new Post();
    post.board = board;
    post.authorId = authorId;
    post.authorNickname = authorNickname;
    post.title = title;
    post.content = content;
    return post;
  }

  public void edit(String title, String content) {
    if (title != null) this.title = title;
    if (content != null) this.content = content;
  }

  public boolean isAuthor(UUID userId) {
    return this.authorId.equals(userId);
  }
}
