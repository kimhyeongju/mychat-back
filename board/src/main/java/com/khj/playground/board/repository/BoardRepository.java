package com.khj.playground.board.repository;

import com.khj.playground.board.entity.Board;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, UUID> {
  List<Board> findAllByOrderByDisplayOrderAsc();

  Optional<Board> findBySlug(String slug);
}
