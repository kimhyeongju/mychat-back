package com.khj.playground.board.controller;

import com.khj.playground.board.dto.BoardResponse;
import com.khj.playground.board.service.BoardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

  private final BoardService boardService;

  @GetMapping
  public List<BoardResponse> list() {
    return boardService.listBoards();
  }

  @GetMapping("/{slug}")
  public BoardResponse get(@PathVariable("slug") String slug) {
    return boardService.getBoard(slug);
  }
}
