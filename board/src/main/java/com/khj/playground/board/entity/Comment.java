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
@Table(name = "board_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "author_nickname", nullable = false, length = 12)
  private String authorNickname;

  @Column(nullable = false, length = 1000)
  private String content;

  public static Comment create(
    Post post,
    UUID authorId,
    String authorNickname,
    String content
  ) {
    Comment comment = new Comment();
    comment.post = post;
    comment.authorId = authorId;
    comment.authorNickname = authorNickname;
    comment.content = content;
    return comment;
  }

  public void edit(String content) {
    this.content = content;
  }

  public boolean isAuthor(UUID userId) {
    return this.authorId.equals(userId);
  }
}
