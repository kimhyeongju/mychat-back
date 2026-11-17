package com.khj.playground.auth.service;

import com.khj.playground.common.exception.ApiException;
import com.khj.playground.common.exception.ErrorCode;
import com.khj.playground.auth.exception.AuthErrorCode;
import com.khj.playground.auth.dto.AdminCreateUserRequest;
import com.khj.playground.auth.dto.AdminUpdateUserRequest;
import com.khj.playground.auth.dto.AdminUserResponse;
import com.khj.playground.auth.entity.User;
import com.khj.playground.auth.enums.Role;
import com.khj.playground.auth.repository.UserRepository;
import com.khj.playground.common.dto.PageResponse;
import com.khj.playground.common.event.UserWithdrawnEvent;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자 전용 회원 관리. 컨트롤러가 ADMIN 권한을 확인한 뒤에만 호출된다.
 *
 * 안전장치
 * - 이 화면에서 만드는 계정은 항상 일반 사용자(USER)다. 다른 계정을 운영자로 올리는 기능은 없다.
 * - 운영자 계정은 수정·삭제할 수 없다(자기 자신 포함). 운영자 실수로 관리자가 없어지는 일을 막는다.
 * - 삭제는 회원 탈퇴와 같은 경로다. 모의투자 계좌·기록 등은 UserWithdrawnEvent 로 정리된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public PageResponse<AdminUserResponse> list(String keyword, int page, int size) {
    PageRequest pageable = PageRequest.of(
      Math.max(page, 0),
      Math.min(Math.max(size, 1), 50),
      Sort.by(Sort.Direction.DESC, "createdAt")
    );
    String q = keyword == null ? "" : keyword.trim();
    // LIKE 의 와일드카드가 검색어로 들어와 전체가 걸리는 것을 막는다.
    q = q.replace("%", "").replace("_", "").replace("\\", "");
    var result = q.isEmpty()
      ? userRepository.findAll(pageable)
      : userRepository.search(q, pageable);
    return PageResponse.of(result, AdminUserResponse::from);
  }

  @Transactional
  public AdminUserResponse create(AdminCreateUserRequest request, String adminUsername) {
    if (userRepository.existsByUsername(request.username())) {
      throw conflict(AuthErrorCode.USERNAME_DUPLICATED);
    }
    if (userRepository.existsByNickname(request.nickname())) {
      throw conflict(AuthErrorCode.NICKNAME_DUPLICATED);
    }

    String phone = request.phoneNumber();
    if (phone == null || phone.isBlank()) {
      phone = generatePlaceholderPhone();
    } else if (userRepository.existsByPhoneNumber(phone)) {
      throw conflict(AuthErrorCode.PHONE_DUPLICATED);
    }

    User user = userRepository.save(
      User
        .builder()
        .username(request.username())
        .password(passwordEncoder.encode(request.password()))
        .nickname(request.nickname())
        .phoneNumber(phone)
        .email(blankToNull(request.email()))
        .role(Role.USER)
        .build()
    );
    log.info("admin_user_created admin={} target={}", adminUsername, user.getUsername());
    return AdminUserResponse.from(user);
  }

  @Transactional
  public AdminUserResponse update(
    UUID id,
    AdminUpdateUserRequest request,
    String adminUsername
  ) {
    User user = find(id);
    requireNotAdmin(user, AuthErrorCode.ADMIN_CANNOT_MODIFY);

    if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
      if (userRepository.existsByNickname(request.nickname())) {
        throw conflict(AuthErrorCode.NICKNAME_DUPLICATED);
      }
      user.changeNickname(request.nickname());
    }
    if (request.phoneNumber() != null && !request.phoneNumber().equals(user.getPhoneNumber())) {
      if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
        throw conflict(AuthErrorCode.PHONE_DUPLICATED);
      }
      user.changePhoneNumber(request.phoneNumber());
    }
    if (request.email() != null) {
      user.changeEmail(blankToNull(request.email()));
    }
    if (request.password() != null) {
      user.changePassword(passwordEncoder.encode(request.password()));
      // 이전 비밀번호로 받은 로그인 세션을 끊는다.
      refreshTokenService.delete(user.getUsername());
    }
    log.info("admin_user_updated admin={} target={}", adminUsername, user.getUsername());
    return AdminUserResponse.from(user);
  }

  @Transactional
  public void delete(UUID id, String adminUsername) {
    User user = find(id);
    requireNotAdmin(user, AuthErrorCode.ADMIN_CANNOT_DELETE);

    // 회원 탈퇴와 같다. 모의투자 계좌·거래·관심 종목·주간 성적이 함께 지워진다.
    eventPublisher.publishEvent(new UserWithdrawnEvent(user.getId()));
    userRepository.delete(user);
    refreshTokenService.delete(user.getUsername());
    log.info("admin_user_deleted admin={} target={}", adminUsername, user.getUsername());
  }

  private User find(UUID id) {
    return userRepository
      .findById(id)
      .orElseThrow(() ->
        new ApiException(AuthErrorCode.USER_NOT_FOUND)
      );
  }

  private static void requireNotAdmin(User user, ErrorCode code) {
    if (user.getRole() == Role.ADMIN) {
      throw new ApiException(code);
    }
  }

  /**
   * 실제 번호와 겹치지 않는 임시 번호. 실제 번호는 01로 시작하므로 00으로 시작하는 11자리를 쓴다.
   * 나중에 수정 화면에서 진짜 번호로 바꿀 수 있다.
   */
  private String generatePlaceholderPhone() {
    for (int i = 0; i < 20; i++) {
      String candidate = "00" + String.format("%09d", RANDOM.nextInt(1_000_000_000));
      if (!userRepository.existsByPhoneNumber(candidate)) {
        return candidate;
      }
    }
    throw new ApiException(AuthErrorCode.PHONE_GENERATION_FAILED);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static ApiException conflict(ErrorCode code) {
    return new ApiException(code);
  }
}
