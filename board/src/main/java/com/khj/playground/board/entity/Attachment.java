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
@Table(name = "board_attachments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  /** 사용자에게 보여주고 다운로드 시 파일명으로 쓴다. */
  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  /** 디스크에 실제로 저장된 이름. UUID + 확장자. */
  @Column(name = "stored_name", nullable = false, length = 100)
  private String storedName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  public static Attachment create(
    Post post,
    String originalName,
    String storedName,
    String contentType,
    long sizeBytes
  ) {
    Attachment attachment = new Attachment();
    attachment.post = post;
    attachment.originalName = originalName;
    attachment.storedName = storedName;
    attachment.contentType = contentType;
    attachment.sizeBytes = sizeBytes;
    return attachment;
  }
}
