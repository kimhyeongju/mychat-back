package com.khj.playground.board.controller;

import com.khj.playground.board.dto.PostDetailResponse;
import com.khj.playground.board.dto.PostRequest;
import com.khj.playground.board.dto.PostSummaryResponse;
import com.khj.playground.board.service.PostService;
import com.khj.playground.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards/{slug}/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;

  @GetMapping
  public PageResponse<PostSummaryResponse> list(
    @PathVariable("slug") String slug,
    @RequestParam(name = "keyword", required = false) String keyword,
    @PageableDefault(size = 20) Pageable pageable
  ) {
    return postService.listPosts(slug, keyword, pageable);
  }

  @GetMapping("/{postId}")
  public PostDetailResponse get(
    @PathVariable("slug") String slug,
    @PathVariable("postId") UUID postId,
    Authentication authentication,
    HttpServletRequest request
  ) {
    return postService.getPost(postId, authentication, request);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostDetailResponse create(
    @PathVariable("slug") String slug,
    @Valid @RequestBody PostRequest request,
    Authentication authentication
  ) {
    return postService.createPost(slug, request, authentication);
  }

  @PatchMapping("/{postId}")
  public PostDetailResponse update(
    @PathVariable("slug") String slug,
    @PathVariable("postId") UUID postId,
    @Valid @RequestBody PostRequest request,
    Authentication authentication
  ) {
    return postService.updatePost(postId, request, authentication);
  }

  @DeleteMapping("/{postId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
    @PathVariable("slug") String slug,
    @PathVariable("postId") UUID postId,
    Authentication authentication
  ) {
    postService.deletePost(postId, authentication);
  }
}
