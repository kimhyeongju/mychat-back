package com.khj.mychatback.api.user.service;

import com.khj.mychatback.api.user.dto.UserSummaryResponse;
import com.khj.mychatback.entity.jpa.User;
import com.khj.mychatback.repo.jpa.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  /** 닉네임에 keyword가 포함된 회원을 최대 10명까지 검색한다 (본인 제외). */
  public List<UserSummaryResponse> searchByNickname(
    String keyword,
    String myUsername
  ) {
    User me = userRepository
      .findByUsername(myUsername)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "존재하지 않는 사용자입니다."
        )
      );

    return userRepository
      .findTop10ByNicknameContainingIgnoreCaseAndIdNot(keyword, me.getId())
      .stream()
      .map(UserSummaryResponse::from)
      .toList();
  }
}
