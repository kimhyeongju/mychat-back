package com.khj.playground.board.repository;

import com.khj.playground.board.entity.Attachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
  List<Attachment> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
