package com.khj.playground.auth.service;

import com.khj.playground.common.exception.ApiException;
import com.khj.playground.auth.exception.AuthErrorCode;
import com.khj.playground.auth.dto.AvailabilityResponse;
import com.khj.playground.auth.dto.FindIdRequest;
import com.khj.playground.auth.dto.FindIdResponse;
import com.khj.playground.auth.dto.LoginRequest;
import com.khj.playground.auth.dto.ResetPasswordRequest;
import com.khj.playground.auth.dto.SignUpRequest;
import com.khj.playground.auth.dto.SignUpResponse;
import com.khj.playground.auth.dto.TokenResponse;
import com.khj.playground.auth.entity.User;
import com.khj.playground.auth.jwt.JwtTokenProvider;
import com.khj.playground.auth.repository.UserRepository;
import com.khj.playground.common.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final PhoneVerificationService phoneVerificationService;
  private final RefreshTokenService refreshTokenService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public SignUpResponse signUp(SignUpRequest request) {
    if (!phoneVerificationService.isVerified(request.phoneNumber())) {
      throw new ApiException(AuthErrorCode.PHONE_VERIFICATION_REQUIRED);
    }
    if (userRepository.existsByUsername(request.username())) {
      throw new ApiException(AuthErrorCode.USERNAME_DUPLICATED);
    }
    if (userRepository.existsByNickname(request.nickname())) {
      throw new ApiException(AuthErrorCode.NICKNAME_DUPLICATED);
    }
    if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
      throw new ApiException(AuthErrorCode.PHONE_DUPLICATED);
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
        new ApiException(AuthErrorCode.LOGIN_FAILED)
      );

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new ApiException(AuthErrorCode.LOGIN_FAILED);
    }

    return issueTokens(user);
  }

  public TokenResponse reissue(String refreshToken) {
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new ApiException(AuthErrorCode.TOKEN_INVALID);
    }

    String username = jwtTokenProvider.getUsername(refreshToken);

    if (!refreshTokenService.isValid(username, refreshToken)) {
      throw new ApiException(AuthErrorCode.TOKEN_REVOKED);
    }

    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() ->
        new ApiException(AuthErrorCode.USER_SESSION_INVALID)
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
      throw new ApiException(AuthErrorCode.PHONE_VERIFICATION_REQUIRED);
    }

    User user = userRepository
      .findByPhoneNumber(request.phoneNumber())
      .orElseThrow(() ->
        new ApiException(AuthErrorCode.ACCOUNT_NOT_FOUND_BY_PHONE)
      );

    phoneVerificationService.clearVerified(request.phoneNumber());

    return new FindIdResponse(maskUsername(user.getUsername()));
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    if (!phoneVerificationService.isVerified(request.phoneNumber())) {
      throw new ApiException(AuthErrorCode.PHONE_VERIFICATION_REQUIRED);
    }

    User user = userRepository
      .findByUsername(request.username())
      .orElseThrow(() ->
        new ApiException(AuthErrorCode.USERNAME_NOT_FOUND)
      );

    if (!user.getPhoneNumber().equals(request.phoneNumber())) {
      throw new ApiException(AuthErrorCode.FIND_ACCOUNT_MISMATCH);
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
        new ApiException(AuthErrorCode.USER_SESSION_INVALID)
      );

    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
      throw new ApiException(AuthErrorCode.PASSWORD_INCORRECT);
    }

    // TODO: 탈퇴 시 ChatRoomMember/ChatMessage는 삭제하지 않고 "익명화" 처리할 예정.
    //       예: user 참조를 끊고 nickname을 "(탈퇴한 사용자)"로 고정, anonymousId 형태로 전환.
    //       이렇게 하면 다른 사용자가 보던 채팅 기록(메시지 자체)은 유지되면서 개인정보만 제거됨.
    //       User 삭제 전에 해당 회원의 모든 ChatRoomMember를 찾아 위 처리를 먼저 수행해야 함 (미구현).
    // 다른 모듈(모의투자 등)이 가진 이 회원의 데이터를 같은 트랜잭션에서 정리한다.
    eventPublisher.publishEvent(new UserWithdrawnEvent(user.getId()));
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
