package com.khj.mychatback.repo.jpa;

import com.khj.mychatback.entity.jpa.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  Optional<User> findByPhoneNumber(String phoneNumber);

  boolean existsByUsername(String username);

  boolean existsByNickname(String nickname);

  boolean existsByPhoneNumber(String phoneNumber);

  List<User> findTop10ByNicknameContainingIgnoreCaseAndIdNot(
    String keyword,
    Long excludeId
  );
}
