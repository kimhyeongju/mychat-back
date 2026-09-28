package com.khj.playground.board.service;

import com.khj.playground.board.dto.CommentRequest;
import com.khj.playground.board.dto.CommentResponse;
import com.khj.playground.board.entity.Comment;
import com.khj.playground.board.entity.Post;
import com.khj.playground.board.repository.CommentRepository;
import com.khj.playground.board.repository.PostRepository;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final CurrentUserProvider currentUserProvider;

  public List<CommentResponse> listComments(
    UUID postId,
    Authentication authentication
  ) {
    // 글이 없으면 404를 내야 하므로 존재 확인을 먼저 한다.
    requirePost(postId);

    UUID viewerId = resolveViewerId(authentication);
    boolean admin = isAdmin(authentication);

    return commentRepository
      .findByPostIdOrderByCreatedAtAsc(postId)
      .stream()
      .map(comment ->
        CommentResponse.from(
          comment,
          admin || (viewerId != null && comment.isAuthor(viewerId))
        )
      )
      .toList();
  }

  @Transactional
  public CommentResponse createComment(
    UUID postId,
    CommentRequest request,
    Authentication authentication
  ) {
    Post post = requirePost(postId);
    AuthenticatedUser user = currentUserProvider.resolve(authentication);

    Comment comment = Comment.create(
      post,
      user.id(),
      user.nickname(),
      request.content()
    );
    commentRepository.save(comment);
    postRepository.increaseCommentCount(postId);

    return CommentResponse.from(comment, true);
  }

  @Transactional
  public CommentResponse updateComment(
    UUID commentId,
    CommentRequest request,
    Authentication authentication
  ) {
    Comment comment = requireComment(commentId);
    requireEditable(comment, authentication);

    comment.edit(request.content());
    return CommentResponse.from(comment, true);
  }

  @Transactional
  public void deleteComment(UUID commentId, Authentication authentication) {
    Comment comment = requireComment(commentId);
    requireEditable(comment, authentication);

    UUID postId = comment.getPost().getId();
    commentRepository.delete(comment);
    postRepository.decreaseCommentCount(postId);
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

  private Comment requireComment(UUID commentId) {
    return commentRepository
      .findById(commentId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 댓글입니다."
        )
      );
  }

  /** 비로그인 조회에서도 호출되므로 예외 대신 null을 반환한다. */
  private UUID resolveViewerId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    try {
      return currentUserProvider.resolve(authentication).id();
    } catch (ResponseStatusException e) {
      return null;
    }
  }

  private boolean isAdmin(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    try {
      return currentUserProvider.resolve(authentication).admin();
    } catch (ResponseStatusException e) {
      return false;
    }
  }

  private void requireEditable(Comment comment, Authentication authentication) {
    AuthenticatedUser user = currentUserProvider.resolve(authentication);
    if (!comment.isAuthor(user.id()) && !user.admin()) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "권한이 없습니다."
      );
    }
  }
}
