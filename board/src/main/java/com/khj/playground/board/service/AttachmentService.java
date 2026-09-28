package com.khj.playground.board.service;

import com.khj.playground.board.dto.AttachmentResponse;
import com.khj.playground.board.entity.Attachment;
import com.khj.playground.board.entity.Post;
import com.khj.playground.board.repository.AttachmentRepository;
import com.khj.playground.board.repository.PostRepository;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AttachmentService {

  /** 글 하나에 붙일 수 있는 첨부 개수 */
  private static final int MAX_COUNT_PER_POST = 5;

  private final AttachmentRepository attachmentRepository;
  private final PostRepository postRepository;
  private final FileStorage fileStorage;
  private final CurrentUserProvider currentUserProvider;

  public List<AttachmentResponse> listByPost(UUID postId) {
    return attachmentRepository
      .findByPostIdOrderByCreatedAtAsc(postId)
      .stream()
      .map(AttachmentResponse::from)
      .toList();
  }

  @Transactional
  public AttachmentResponse upload(
    UUID postId,
    MultipartFile file,
    Authentication authentication
  ) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "빈 파일입니다."
      );
    }

    Post post = requirePost(postId);
    requireEditable(post, authentication);

    if (
      attachmentRepository.findByPostIdOrderByCreatedAtAsc(postId).size() >=
      MAX_COUNT_PER_POST
    ) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "첨부는 최대 " + MAX_COUNT_PER_POST + "개까지 가능합니다."
      );
    }

    String storedName = fileStorage.store(file);

    Attachment attachment = Attachment.create(
      post,
      file.getOriginalFilename(),
      storedName,
      file.getContentType() != null
        ? file.getContentType()
        : "application/octet-stream",
      file.getSize()
    );
    attachmentRepository.save(attachment);

    return AttachmentResponse.from(attachment);
  }

  public DownloadTarget download(UUID attachmentId) {
    Attachment attachment = requireAttachment(attachmentId);
    Resource resource = fileStorage.load(attachment.getStoredName());
    return new DownloadTarget(
      resource,
      attachment.getOriginalName(),
      attachment.getContentType()
    );
  }

  @Transactional
  public void delete(UUID attachmentId, Authentication authentication) {
    Attachment attachment = requireAttachment(attachmentId);
    requireEditable(attachment.getPost(), authentication);

    String storedName = attachment.getStoredName();
    attachmentRepository.delete(attachment);
    // DB 삭제가 확정된 뒤 디스크에서 지운다.
    fileStorage.delete(storedName);
  }

  private Post requirePost(UUID postId) {
    return postRepository
      .findById(postId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 글입니다."
        )
      );
  }

  private Attachment requireAttachment(UUID attachmentId) {
    return attachmentRepository
      .findById(attachmentId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 첨부입니다."
        )
      );
  }

  private void requireEditable(Post post, Authentication authentication) {
    AuthenticatedUser user = currentUserProvider.resolve(authentication);
    if (!post.isAuthor(user.id()) && !user.admin()) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "권한이 없습니다."
      );
    }
  }

  public record DownloadTarget(
    Resource resource,
    String filename,
    String contentType
  ) {}

  /**
   * 글 삭제 시 호출한다. DB 행은 FK CASCADE로 지워지지만
   * 디스크 파일은 여기서 직접 정리해야 고아 파일이 남지 않는다.
   */
  @Transactional
  public void deleteFilesByPost(UUID postId) {
    attachmentRepository
      .findByPostIdOrderByCreatedAtAsc(postId)
      .forEach(attachment -> fileStorage.delete(attachment.getStoredName()));
  }

  /**
   * 글 삭제 시 호출한다.
   * DB에는 FK CASCADE가 걸려 있지만, 영속성 컨텍스트에 남은 Attachment가
   * 삭제 대기 중인 Post를 참조하면 flush에서 실패하므로 JPA로도 함께 지운다.
   */
  @Transactional
  public void deleteAllByPost(UUID postId) {
    List<Attachment> attachments = attachmentRepository.findByPostIdOrderByCreatedAtAsc(
      postId
    );

    attachments.forEach(a -> fileStorage.delete(a.getStoredName()));
    attachmentRepository.deleteAll(attachments);
  }
}
