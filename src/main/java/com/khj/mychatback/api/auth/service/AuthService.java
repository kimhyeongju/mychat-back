package com.khj.mychatback.api.auth.service;

import com.khj.mychatback.api.auth.dto.AvailabilityResponse;
import com.khj.mychatback.api.auth.dto.FindIdRequest;
import com.khj.mychatback.api.auth.dto.FindIdResponse;
import com.khj.mychatback.api.auth.dto.LoginRequest;
import com.khj.mychatback.api.auth.dto.ResetPasswordRequest;
import com.khj.mychatback.api.auth.dto.SignUpRequest;
import com.khj.mychatback.api.auth.dto.SignUpResponse;
import com.khj.mychatback.api.auth.dto.TokenResponse;
import com.khj.mychatback.config.jwt.JwtTokenProvider;
import com.khj.mychatback.entity.jpa.User;
import com.khj.mychatback.repo.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final PhoneVerificationService phoneVerificationService;
  private final RefreshTokenService refreshTokenService;

  @Transactional
  public SignUpResponse signUp(SignUpRequest request) {
    if (!phoneVerificationService.isVerified(request.phoneNumber())) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "휴대폰 인증을 먼저 완료해주세요."
      );
    }
    if (userRepository.existsByUsername(request.username())) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "이미 사용 중인 아이디입니다."
      );
    }
    if (userRepository.existsByNickname(request.nickname())) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "이미 사용 중인 닉네임입니다."
      );
    }
    if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "이미 가입된 휴대폰 번호입니다."
      );
    }

    User user = User
      .builder()
      .username(request.username())
      .password(passwordEncoder.encode(request.password()))
      .nickname(request.nickname())
      .phoneNumber(request.phoneNumber())
      .email(request.email())
      .build();

    User saved = userRepository.save(user);

    // 회원가입이 끝났으니 "인증 완료" 임시 상태는 정리 (동일 번호로 재가입 시도 방지)
    phoneVerificationService.clearVerified(request.phoneNumber());

    return SignUpResponse.from(saved);
  }

  public TokenResponse login(LoginRequest request) {
    User user = userRepository
      .findByUsername(request.username())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "아이디 또는 비밀번호가 올바르지 않습니다."
        )
      );

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "아이디 또는 비밀번호가 올바르지 않습니다."
      );
    }

    return issueTokens(user);
  }

  public TokenResponse reissue(String refreshToken) {
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "만료되었거나 유효하지 않은 토큰입니다."
      );
    }

    String username = jwtTokenProvider.getUsername(refreshToken);

    if (!refreshTokenService.isValid(username, refreshToken)) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "만료되었거나 이미 폐기된 토큰입니다."
      );
    }

    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "존재하지 않는 사용자입니다."
        )
      );

    return issueTokens(user);
  }

  public void logout(String username) {
    refreshTokenService.delete(username);
  }

  public AvailabilityResponse checkUsernameAvailable(String username) {
    return new AvailabilityResponse(!userRepository.existsByUsername(username));
  }

  public AvailabilityResponse checkNicknameAvailable(String nickname) {
    return new AvailabilityResponse(!userRepository.existsByNickname(nickname));
  }

  public FindIdResponse findId(FindIdRequest request) {
    if (!phoneVerificationService.isVerified(request.phoneNumber())) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "휴대폰 인증을 먼저 완료해주세요."
      );
    }

    User user = userRepository
      .findByPhoneNumber(request.phoneNumber())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "해당 번호로 가입된 계정이 없습니다."
        )
      );

    phoneVerificationService.clearVerified(request.phoneNumber());

    return new FindIdResponse(maskUsername(user.getUsername()));
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    if (!phoneVerificationService.isVerified(request.phoneNumber())) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "휴대폰 인증을 먼저 완료해주세요."
      );
    }

    User user = userRepository
      .findByUsername(request.username())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 아이디입니다."
        )
      );

    if (!user.getPhoneNumber().equals(request.phoneNumber())) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "아이디와 휴대폰 번호가 일치하지 않습니다."
      );
    }

    user.changePassword(passwordEncoder.encode(request.newPassword()));
    phoneVerificationService.clearVerified(request.phoneNumber());

    // 비밀번호가 바뀌었으니 기존에 발급된 refresh token은 전부 무효화한다.
    refreshTokenService.delete(user.getUsername());
  }

  @Transactional
  public void withdraw(String username, String rawPassword) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "존재하지 않는 사용자입니다."
        )
      );

    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "비밀번호가 올바르지 않습니다."
      );
    }

    // TODO: 탈퇴 시 ChatRoomMember/ChatMessage는 삭제하지 않고 "익명화" 처리할 예정.
    //       예: user 참조를 끊고 nickname을 "(탈퇴한 사용자)"로 고정, anonymousId 형태로 전환.
    //       이렇게 하면 다른 사용자가 보던 채팅 기록(메시지 자체)은 유지되면서 개인정보만 제거됨.
    //       User 삭제 전에 해당 회원의 모든 ChatRoomMember를 찾아 위 처리를 먼저 수행해야 함 (미구현).
    userRepository.delete(user);
    refreshTokenService.delete(username);
  }

  private String maskUsername(String username) {
    if (username.length() <= 2) {
      return username.charAt(0) + "*".repeat(username.length() - 1);
    }
    int visibleLength = Math.min(2, username.length() - 2);
    return (
      username.substring(0, visibleLength) +
      "*".repeat(username.length() - visibleLength)
    );
  }

  private TokenResponse issueTokens(User user) {
    String accessToken = jwtTokenProvider.createAccessToken(
      user.getUsername(),
      user.getRole()
    );
    String refreshToken = jwtTokenProvider.createRefreshToken(
      user.getUsername()
    );

    refreshTokenService.save(user.getUsername(), refreshToken);

    return TokenResponse.of(accessToken, refreshToken);
  }
}
