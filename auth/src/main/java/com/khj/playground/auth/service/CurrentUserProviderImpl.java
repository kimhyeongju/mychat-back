package com.khj.playground.auth.service;

import com.khj.playground.common.exception.ApiException;
import com.khj.playground.auth.exception.AuthErrorCode;
import com.khj.playground.common.exception.CommonErrorCode;
import com.khj.playground.auth.entity.User;
import com.khj.playground.auth.enums.Role;
import com.khj.playground.auth.repository.UserRepository;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserProviderImpl implements CurrentUserProvider {

  private final UserRepository userRepository;

  @Override
  public AuthenticatedUser resolve(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ApiException(CommonErrorCode.UNAUTHORIZED);
    }

    User user = userRepository
      .findByUsername(authentication.getName())
      .orElseThrow(() ->
        new ApiException(AuthErrorCode.USER_SESSION_INVALID)
      );

    return new AuthenticatedUser(
      user.getId(),
      user.getNickname(),
      user.getRole() == Role.ADMIN
    );
  }
}
