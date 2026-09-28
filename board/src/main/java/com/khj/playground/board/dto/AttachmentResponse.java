package com.khj.playground.board.dto;

import com.khj.playground.board.entity.Attachment;
import java.util.UUID;

public record AttachmentResponse(
  UUID id,
  String originalName,
  String contentType,
  long sizeBytes
) {
  public static AttachmentResponse from(Attachment attachment) {
    return new AttachmentResponse(
      attachment.getId(),
      attachment.getOriginalName(),
      attachment.getContentType(),
      attachment.getSizeBytes()
    );
  }
}
