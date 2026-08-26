package com.khj.mychatback.repo.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.khj.mychatback.entity.jpa.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    boolean existsByPhoneNumber(String phoneNumber);
}
