package com.khj.playground.chat.repository;

import com.khj.playground.chat.entity.AnonymousSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnonymousSessionRepository
  extends JpaRepository<AnonymousSession, UUID> {}
