package com.khj.playground.auth.service;

import com.khj.playground.auth.enums.Role;
import com.khj.playground.auth.repository.UserRepository;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLookupImpl implements MemberLookup {

  private final UserRepository userRepository;

  @Override
  public Optional<AuthenticatedUser> findById(UUID userId) {
    return userRepository
      .findById(userId)
      .map(user ->
        new AuthenticatedUser(
          user.getId(),
          user.getNickname(),
          user.getRole() == Role.ADMIN
        )
      );
  }
}
