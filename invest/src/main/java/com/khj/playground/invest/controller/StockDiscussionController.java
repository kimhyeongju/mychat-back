package com.khj.playground.invest.controller;

import com.khj.playground.common.dto.PageResponse;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import com.khj.playground.invest.dto.LikeResponse;
import com.khj.playground.invest.dto.StockCommentRequest;
import com.khj.playground.invest.dto.StockCommentResponse;
import com.khj.playground.invest.dto.StockPostRequest;
import com.khj.playground.invest.dto.StockPostResponse;
import com.khj.playground.invest.service.StockDiscussionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 종목 토론. 조회(GET)는 비로그인도 가능하고, 로그인 상태면 liked/mine 이 채워진다. */
@RestController
@RequestMapping("/api/invest")
@RequiredArgsConstructor
public class StockDiscussionController {

  private final StockDiscussionService discussionService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping("/symbols/{symbol}/posts")
  public PageResponse<StockPostResponse> posts(
    @PathVariable("symbol") String symbol,
    @RequestParam(name = "sort", defaultValue = "latest") String sort,
    @RequestParam(name = "page", defaultValue = "0") int page,
    @RequestParam(name = "size", defaultValue = "20") int size,
    Authentication authentication
  ) {
    return discussionService.listPosts(
      symbol,
      "popular".equalsIgnoreCase(sort),
      page,
      size,
      optionalUser(authentication)
    );
  }

  @PostMapping("/symbols/{symbol}/posts")
  @ResponseStatus(HttpStatus.CREATED)
  public StockPostResponse createPost(
    @PathVariable("symbol") String symbol,
    @Valid @RequestBody StockPostRequest request,
    Authentication authentication
  ) {
    return discussionService.createPost(user(authentication), symbol, request.content());
  }

  @PatchMapping("/posts/{postId}")
  public StockPostResponse editPost(
    @PathVariable("postId") Long postId,
    @Valid @RequestBody StockPostRequest request,
    Authentication authentication
  ) {
    return discussionService.editPost(user(authentication), postId, request.content());
  }

  @DeleteMapping("/posts/{postId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePost(
    @PathVariable("postId") Long postId,
    Authentication authentication
  ) {
    discussionService.deletePost(user(authentication), postId);
  }

  @PutMapping("/posts/{postId}/like")
  public LikeResponse like(
    @PathVariable("postId") Long postId,
    Authentication authentication
  ) {
    return discussionService.like(user(authentication), postId);
  }

  @DeleteMapping("/posts/{postId}/like")
  public LikeResponse unlike(
    @PathVariable("postId") Long postId,
    Authentication authentication
  ) {
    return discussionService.unlike(user(authentication), postId);
  }

  @GetMapping("/posts/{postId}/comments")
  public List<StockCommentResponse> comments(
    @PathVariable("postId") Long postId,
    Authentication authentication
  ) {
    return discussionService.listComments(postId, optionalUser(authentication));
  }

  @PostMapping("/posts/{postId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public StockCommentResponse createComment(
    @PathVariable("postId") Long postId,
    @Valid @RequestBody StockCommentRequest request,
    Authentication authentication
  ) {
    return discussionService.createComment(user(authentication), postId, request.content());
  }

  @DeleteMapping("/comments/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComment(
    @PathVariable("commentId") Long commentId,
    Authentication authentication
  ) {
    discussionService.deleteComment(user(authentication), commentId);
  }

  private AuthenticatedUser user(Authentication authentication) {
    return currentUserProvider.resolve(authentication);
  }

  private AuthenticatedUser optionalUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    try {
      return currentUserProvider.resolve(authentication);
    } catch (ResponseStatusException e) {
      return null;
    }
  }
}
