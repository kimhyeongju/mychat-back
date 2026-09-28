package com.khj.playground.board.dto;

import com.khj.playground.board.entity.Board;

public record BoardResponse(
  String slug,
  String name,
  String description,
  boolean adminOnly
) {
  public static BoardResponse from(Board board) {
    return new BoardResponse(
      board.getSlug(),
      board.getName(),
      board.getDescription(),
      board.isAdminOnly()
    );
  }
}
