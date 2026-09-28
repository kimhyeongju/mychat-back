package com.khj.playground.board.controller;

import com.khj.playground.board.dto.CommentRequest;
import com.khj.playground.board.dto.CommentResponse;
import com.khj.playground.board.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @GetMapping
  public List<CommentResponse> list(
    @PathVariable("postId") UUID postId,
    Authentication authentication
  ) {
    return commentService.listComments(postId, authentication);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse create(
    @PathVariable("postId") UUID postId,
    @Valid @RequestBody CommentRequest request,
    Authentication authentication
  ) {
    return commentService.createComment(postId, request, authentication);
  }

  @PatchMapping("/{commentId}")
  public CommentResponse update(
    @PathVariable("postId") UUID postId,
    @PathVariable("commentId") UUID commentId,
    @Valid @RequestBody CommentRequest request,
    Authentication authentication
  ) {
    return commentService.updateComment(commentId, request, authentication);
  }

  @DeleteMapping("/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
    @PathVariable("postId") UUID postId,
    @PathVariable("commentId") UUID commentId,
    Authentication authentication
  ) {
    commentService.deleteComment(commentId, authentication);
  }
}
