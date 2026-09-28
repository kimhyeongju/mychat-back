package com.khj.playground.board.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class FileStorage {

  /** 허용 확장자. 실행 가능한 형식은 절대 받지 않는다. */
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
    "jpg",
    "jpeg",
    "png",
    "gif",
    "pdf",
    "txt",
    "csv",
    "zip",
    "hwp",
    "docx",
    "xlsx",
    "pptx"
  );

  private final Path root;

  public FileStorage(@Value("${app.upload.dir}") String uploadDir) {
    this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(root);
    } catch (IOException e) {
      throw new IllegalStateException(
        "업로드 디렉터리를 만들 수 없습니다: " + root,
        e
      );
    }
  }

  /**
   * @return 디스크에 저장된 파일명 (UUID + 확장자)
   */
  public String store(MultipartFile file) {
    String extension = extractExtension(file.getOriginalFilename());
    String storedName = UUID.randomUUID() + "." + extension;

    // 저장 경로가 root를 벗어나지 않는지 최종 확인한다.
    Path target = root.resolve(storedName).normalize();
    if (!target.startsWith(root)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "잘못된 파일명입니다."
      );
    }

    try (var in = file.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "파일 저장에 실패했습니다."
      );
    }

    return storedName;
  }

  public Resource load(String storedName) {
    Path target = root.resolve(storedName).normalize();
    if (!target.startsWith(root) || !Files.exists(target)) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "파일을 찾을 수 없습니다."
      );
    }

    try {
      return new UrlResource(target.toUri());
    } catch (IOException e) {
      throw new ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "파일을 읽을 수 없습니다."
      );
    }
  }

  public void delete(String storedName) {
    Path target = root.resolve(storedName).normalize();
    if (!target.startsWith(root)) return;

    try {
      Files.deleteIfExists(target);
    } catch (IOException e) {
      // 파일이 남아도 DB 정합성이 더 중요하므로 예외를 던지지 않는다.
      log.warn("첨부 파일 삭제 실패: {}", storedName, e);
    }
  }

  private String extractExtension(String originalFilename) {
    if (originalFilename == null || !originalFilename.contains(".")) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "확장자가 없는 파일은 업로드할 수 없습니다."
      );
    }

    String extension = originalFilename
      .substring(originalFilename.lastIndexOf('.') + 1)
      .toLowerCase(Locale.ROOT);

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "허용되지 않는 파일 형식입니다: " + extension
      );
    }

    return extension;
  }
}
