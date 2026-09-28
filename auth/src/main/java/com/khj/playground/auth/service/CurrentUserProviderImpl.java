package com.khj.playground.auth.service;

import com.khj.playground.auth.entity.User;
import com.khj.playground.auth.enums.Role;
import com.khj.playground.auth.repository.UserRepository;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {

  private final UserRepository userRepository;

  @Override
  public AuthenticatedUser resolve(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "로그인이 필요합니다."
      );
    }

    User user = userRepository
      .findByUsername(authentication.getName())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "존재하지 않는 사용자입니다."
        )
      );

    return new AuthenticatedUser(
      user.getId(),
      user.getNickname(),
      user.getRole() == Role.ADMIN
    );
  }
}
