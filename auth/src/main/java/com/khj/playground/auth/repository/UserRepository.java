package com.khj.playground.auth.repository;

import com.khj.playground.auth.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByUsername(String username);

  Optional<User> findByPhoneNumber(String phoneNumber);

  boolean existsByUsername(String username);

  boolean existsByNickname(String nickname);

  boolean existsByPhoneNumber(String phoneNumber);

  List<User> findTop10ByNicknameContainingIgnoreCaseAndIdNot(
    String keyword,
    UUID excludeId
  );

  /** 운영자 화면 검색. 아이디 또는 닉네임에 keyword 가 포함된 회원. */
  @Query(
    """
    SELECT u FROM User u
    WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """
  )
  Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
