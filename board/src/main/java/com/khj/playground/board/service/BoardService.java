package com.khj.playground.board.service;

import com.khj.playground.common.exception.ApiException;
import com.khj.playground.board.exception.BoardErrorCode;
import com.khj.playground.board.dto.BoardResponse;
import com.khj.playground.board.entity.Board;
import com.khj.playground.board.repository.BoardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardService {

  private final BoardRepository boardRepository;

  public List<BoardResponse> listBoards() {
    return boardRepository
      .findAllByOrderByDisplayOrderAsc()
      .stream()
      .map(BoardResponse::from)
      .toList();
  }

  public BoardResponse getBoard(String slug) {
    return BoardResponse.from(findBySlug(slug));
  }

  /** 다른 서비스(PostService 등)가 엔티티 자체를 필요로 할 때 쓴다. */
  public Board findBySlug(String slug) {
    return boardRepository
      .findBySlug(slug)
      .orElseThrow(() ->
        new ApiException(BoardErrorCode.BOARD_NOT_FOUND)
      );
  }
}
