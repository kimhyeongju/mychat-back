package com.khj.playground.auth.service;

import com.khj.playground.auth.dto.MeResponse;
import com.khj.playground.auth.dto.UpdateProfileRequest;
import com.khj.playground.auth.entity.User;
import com.khj.playground.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileService {

  private final UserRepository userRepository;

  public MeResponse getMe(String username) {
    return MeResponse.from(findByUsername(username));
  }

  @Transactional
  public MeResponse updateProfile(
    String username,
    UpdateProfileRequest request
  ) {
    User user = findByUsername(username);

    if (
      request.nickname() != null &&
      !request.nickname().equals(user.getNickname())
    ) {
      if (userRepository.existsByNickname(request.nickname())) {
        throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "이미 사용 중인 닉네임입니다."
        );
      }
      user.changeNickname(request.nickname());
    }

    if (request.email() != null) {
      user.changeEmail(request.email());
    }

    return MeResponse.from(user);
  }

  private User findByUsername(String username) {
    return userRepository
      .findByUsername(username)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "존재하지 않는 사용자입니다."
        )
      );
  }
}
