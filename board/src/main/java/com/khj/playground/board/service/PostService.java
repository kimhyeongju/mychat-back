package com.khj.playground.board.service;

import com.khj.playground.board.dto.PostDetailResponse;
import com.khj.playground.board.dto.PostRequest;
import com.khj.playground.board.dto.PostSummaryResponse;
import com.khj.playground.board.entity.Board;
import com.khj.playground.board.entity.Post;
import com.khj.playground.board.repository.PostRepository;
import com.khj.playground.common.dto.PageResponse;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PostService {

  private final PostRepository postRepository;
  private final BoardService boardService;
  private final CurrentUserProvider currentUserProvider;
  private final PostViewCounter viewCounter;

  public PageResponse<PostSummaryResponse> listPosts(
    String boardSlug,
    String keyword,
    Pageable pageable
  ) {
    Board board = boardService.findBySlug(boardSlug);

    Page<Post> posts = (keyword == null || keyword.isBlank())
      ? postRepository.findByBoardIdOrderByCreatedAtDesc(
        board.getId(),
        pageable
      )
      : postRepository.findByBoardIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
        board.getId(),
        keyword.trim(),
        pageable
      );

    return PageResponse.of(posts, PostSummaryResponse::from);
  }

  @Transactional
  public PostDetailResponse getPost(
    UUID postId,
    Authentication authentication,
    HttpServletRequest request
  ) {
    Post post = findPost(postId);

    if (
      viewCounter.markViewed(postId, resolveViewerKey(authentication, request))
    ) {
      postRepository.increaseViewCount(postId);
      // 방금 UPDATE한 값이 엔티티에는 반영되지 않았으므로 응답용으로만 +1 한다.
      return PostDetailResponse.from(
        post,
        canEdit(post, authentication),
        post.getViewCount() + 1
      );
    }

    return PostDetailResponse.from(
      post,
      canEdit(post, authentication),
      post.getViewCount()
    );
  }

  /**
   * 로그인 사용자는 회원 ID, 비로그인은 IP로 식별한다.
   * IP는 Cloudflare Tunnel을 거치지만 forward-headers-strategy 설정으로 실제 클라이언트 IP가 들어온다.
   */
  private String resolveViewerKey(
    Authentication authentication,
    HttpServletRequest request
  ) {
    if (authentication != null && authentication.isAuthenticated()) {
      try {
        return "u:" + currentUserProvider.resolve(authentication).id();
      } catch (ResponseStatusException ignored) {
        // 토큰이 유효하지 않은 경우 IP로 폴백
      }
    }
    return "ip:" + request.getRemoteAddr();
  }

  @Transactional
  public PostDetailResponse createPost(
    String boardSlug,
    PostRequest request,
    Authentication authentication
  ) {
    Board board = boardService.findBySlug(boardSlug);
    AuthenticatedUser user = currentUserProvider.resolve(authentication);

    if (board.isAdminOnly() && !user.admin()) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "이 게시판에는 관리자만 글을 쓸 수 있습니다."
      );
    }

    Post post = Post.create(
      board,
      user.id(),
      user.nickname(),
      request.title(),
      request.content()
    );
    postRepository.save(post);

    return PostDetailResponse.from(post, true);
  }

  @Transactional
  public PostDetailResponse updatePost(
    UUID postId,
    PostRequest request,
    Authentication authentication
  ) {
    Post post = findPost(postId);
    requireEditable(post, authentication);

    post.edit(request.title(), request.content());
    return PostDetailResponse.from(post, true);
  }

  @Transactional
  public void deletePost(UUID postId, Authentication authentication) {
    Post post = findPost(postId);
    requireEditable(post, authentication);
    postRepository.delete(post);
  }

  private Post findPost(UUID postId) {
    return postRepository
      .findById(postId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 글입니다."
        )
      );
  }

  /** 비로그인 조회에서도 호출되므로 예외를 던지지 않고 false를 반환한다. */
  private boolean canEdit(Post post, Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    try {
      AuthenticatedUser user = currentUserProvider.resolve(authentication);
      return post.isAuthor(user.id()) || user.admin();
    } catch (ResponseStatusException e) {
      return false;
    }
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
}
