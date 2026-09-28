package com.khj.playground.board.controller;

import com.khj.playground.board.dto.AttachmentResponse;
import com.khj.playground.board.service.AttachmentService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

  private final AttachmentService attachmentService;

  @GetMapping("/api/posts/{postId}/attachments")
  public List<AttachmentResponse> list(@PathVariable("postId") UUID postId) {
    return attachmentService.listByPost(postId);
  }

  @PostMapping(
    value = "/api/posts/{postId}/attachments",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentResponse upload(
    @PathVariable("postId") UUID postId,
    @RequestPart("file") @Schema(
      type = "string",
      format = "binary",
      description = "업로드할 파일"
    ) MultipartFile file,
    Authentication authentication
  ) {
    return attachmentService.upload(postId, file, authentication);
  }

  @GetMapping("/api/attachments/{attachmentId}")
  public ResponseEntity<Resource> download(
    @PathVariable("attachmentId") UUID attachmentId
  ) {
    var target = attachmentService.download(attachmentId);

    // 한글 파일명이 깨지지 않도록 RFC 5987 인코딩을 쓴다.
    ContentDisposition disposition = ContentDisposition
      .attachment()
      .filename(target.filename(), StandardCharsets.UTF_8)
      .build();

    return ResponseEntity
      .ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
      .contentType(MediaType.parseMediaType(target.contentType()))
      .body(target.resource());
  }

  @DeleteMapping("/api/attachments/{attachmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
    @PathVariable("attachmentId") UUID attachmentId,
    Authentication authentication
  ) {
    attachmentService.delete(attachmentId, authentication);
  }
}
