package com.khj.playground.invest.service;

import com.khj.playground.common.dto.PageResponse;
import com.khj.playground.common.exception.ApiException;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import com.khj.playground.invest.dto.LikeResponse;
import com.khj.playground.invest.dto.StockCommentResponse;
import com.khj.playground.invest.dto.StockPostResponse;
import com.khj.playground.invest.entity.StockComment;
import com.khj.playground.invest.entity.StockPost;
import com.khj.playground.invest.entity.StockPostLike;
import com.khj.playground.invest.exception.InvestErrorCode;
import com.khj.playground.invest.repository.StockCommentRepository;
import com.khj.playground.invest.repository.StockPostLikeRepository;
import com.khj.playground.invest.repository.StockPostRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 종목 토론(글·댓글·좋아요). 조회는 누구나, 쓰기는 로그인한 사용자만 한다.
 * 수정·삭제는 작성자 본인이, 삭제는 운영자도 할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class StockDiscussionService {

  static final int MAX_PAGE_SIZE = 50;
  static final int MAX_COMMENTS = 200;
  private static final String WITHDRAWN = "(탈퇴한 사용자)";

  private final StockPostRepository postRepository;
  private final StockCommentRepository commentRepository;
  private final StockPostLikeRepository likeRepository;
  private final MemberLookup memberLookup;
  private final PriceService priceService;
  private final DiscussionRateLimiter rateLimiter;

  // ----- 글 -----

  /** @param popular true 면 좋아요 많은 순, 아니면 최신순 */
  @Transactional(readOnly = true)
  public PageResponse<StockPostResponse> listPosts(
    String rawSymbol,
    boolean popular,
    int page,
    int size,
    AuthenticatedUser viewer
  ) {
    String symbol = priceService.normalize(rawSymbol);
    Sort sort = popular
      ? Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
      : Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    Pageable pageable = PageRequest.of(
      Math.max(page, 0),
      Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
      sort
    );
    Page<StockPost> result = postRepository.findBySymbol(symbol, pageable);

    List<StockPost> posts = result.getContent();
    Map<UUID, String> nicknames = nicknames(posts.stream().map(StockPost::getUserId).toList());
    Set<Long> liked = likedBy(viewer, posts.stream().map(StockPost::getId).toList());
    return PageResponse.of(result, p -> toResponse(p, nicknames, liked, viewer));
  }

  @Transactional(readOnly = true)
  public StockPostResponse getPost(Long postId, AuthenticatedUser viewer) {
    StockPost post = findPost(postId);
    Map<UUID, String> nicknames = nicknames(List.of(post.getUserId()));
    return toResponse(post, nicknames, likedBy(viewer, List.of(postId)), viewer);
  }

  public StockPostResponse createPost(AuthenticatedUser user, String rawSymbol, String content) {
    String symbol = priceService.normalize(rawSymbol);
    // 존재하는 종목인지 확인한다. 없으면 404 가 나가고 저장하지 않는다.
    priceService.getQuote(symbol);
    rateLimiter.checkPost(user.id());
    StockPost saved = postRepository.save(StockPost.of(symbol, user.id(), content.strip()));
    return StockPostResponse.of(saved, user.nickname(), false, true);
  }

  @Transactional
  public StockPostResponse editPost(AuthenticatedUser user, Long postId, String content) {
    StockPost post = findPost(postId);
    // 수정은 운영자도 할 수 없다. 남의 이름으로 글이 바뀌지 않게 한다.
    if (!post.getUserId().equals(user.id())) {
      throw new ApiException(InvestErrorCode.POST_FORBIDDEN);
    }
    post.edit(content.strip());
    return toResponse(
      post,
      Map.of(user.id(), user.nickname()),
      likedBy(user, List.of(postId)),
      user
    );
  }

  public void deletePost(AuthenticatedUser user, Long postId) {
    StockPost post = findPost(postId);
    requireOwnerOrAdmin(user, post.getUserId());
    postRepository.deleteById(postId);
  }

  // ----- 좋아요 -----

  /** 이미 눌렀어도 결과는 같다(멱등). 동시에 두 번 눌러 유니크 제약에 걸려도 무시한다. */
  public LikeResponse like(AuthenticatedUser user, Long postId) {
    findPost(postId);
    if (!likeRepository.existsByPostIdAndUserId(postId, user.id())) {
      try {
        likeRepository.saveAndFlush(StockPostLike.of(postId, user.id()));
      } catch (DataIntegrityViolationException e) {
        // 이미 들어갔다.
      }
      postRepository.recountLikes(postId);
    }
    return new LikeResponse(findPost(postId).getLikeCount(), true);
  }

  public LikeResponse unlike(AuthenticatedUser user, Long postId) {
    findPost(postId);
    if (likeRepository.deleteByPostIdAndUserId(postId, user.id()) > 0) {
      postRepository.recountLikes(postId);
    }
    return new LikeResponse(findPost(postId).getLikeCount(), false);
  }

  // ----- 댓글 -----

  @Transactional(readOnly = true)
  public List<StockCommentResponse> listComments(Long postId, AuthenticatedUser viewer) {
    findPost(postId);
    List<StockComment> comments = commentRepository.findByPostIdOrderByIdAsc(
      postId,
      PageRequest.of(0, MAX_COMMENTS)
    );
    Map<UUID, String> nicknames = nicknames(comments.stream().map(StockComment::getUserId).toList());
    return comments
      .stream()
      .map(c ->
        StockCommentResponse.of(
          c,
          nicknames.getOrDefault(c.getUserId(), WITHDRAWN),
          viewer != null && viewer.id().equals(c.getUserId())
        )
      )
      .toList();
  }

  public StockCommentResponse createComment(AuthenticatedUser user, Long postId, String content) {
    findPost(postId);
    rateLimiter.checkComment(user.id());
    StockComment saved = commentRepository.save(
      StockComment.of(postId, user.id(), content.strip())
    );
    postRepository.recountComments(postId);
    return StockCommentResponse.of(saved, user.nickname(), true);
  }

  public void deleteComment(AuthenticatedUser user, Long commentId) {
    StockComment comment = commentRepository
      .findById(commentId)
      .orElseThrow(() -> new ApiException(InvestErrorCode.COMMENT_NOT_FOUND));
    requireOwnerOrAdmin(user, comment.getUserId());
    commentRepository.deleteById(commentId);
    postRepository.recountComments(comment.getPostId());
  }

  // ----- 내부 -----

  private StockPost findPost(Long postId) {
    return postRepository
      .findById(postId)
      .orElseThrow(() -> new ApiException(InvestErrorCode.POST_NOT_FOUND));
  }

  private void requireOwnerOrAdmin(AuthenticatedUser user, UUID ownerId) {
    if (!user.admin() && !user.id().equals(ownerId)) {
      throw new ApiException(InvestErrorCode.POST_FORBIDDEN);
    }
  }

  private StockPostResponse toResponse(
    StockPost post,
    Map<UUID, String> nicknames,
    Set<Long> liked,
    AuthenticatedUser viewer
  ) {
    return StockPostResponse.of(
      post,
      nicknames.getOrDefault(post.getUserId(), WITHDRAWN),
      liked.contains(post.getId()),
      viewer != null && viewer.id().equals(post.getUserId())
    );
  }

  private Set<Long> likedBy(AuthenticatedUser viewer, List<Long> postIds) {
    if (viewer == null || postIds.isEmpty()) return Set.of();
    return new HashSet<>(likeRepository.findLikedPostIds(viewer.id(), postIds));
  }

  private Map<UUID, String> nicknames(List<UUID> userIds) {
    Map<UUID, String> result = new HashMap<>();
    for (UUID id : new HashSet<>(userIds)) {
      memberLookup.findById(id).ifPresent(m -> result.put(id, m.nickname()));
    }
    return result;
  }
}
