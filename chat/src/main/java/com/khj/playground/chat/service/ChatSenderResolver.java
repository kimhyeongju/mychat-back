package com.khj.playground.chat.service;

import com.khj.playground.chat.entity.AnonymousSession;
import com.khj.playground.chat.enums.SenderType;
import com.khj.playground.chat.repository.AnonymousSessionRepository;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.CurrentUserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 메시지 발신자를 식별한다.
 * 로그인 사용자가 우선이며, 토큰이 없을 때만 익명 세션을 확인한다.
 */
@Component
@RequiredArgsConstructor
public class ChatSenderResolver {

  private final CurrentUserProvider currentUserProvider;
  private final AnonymousSessionRepository anonymousSessionRepository;

  /** 익명 신원을 새로 발급한다. */
  @Transactional
  public AnonymousSession issueAnonymousSession() {
    return anonymousSessionRepository.save(
      AnonymousSession.create(AnonymousNicknameGenerator.generate())
    );
  }

  /**
   * @param anonymousId 클라이언트가 보관한 익명 세션 토큰. 로그인 상태면 무시된다.
   */
  public Sender resolve(Authentication authentication, UUID anonymousId) {
    if (authentication != null && authentication.isAuthenticated()) {
      try {
        AuthenticatedUser user = currentUserProvider.resolve(authentication);
        return new Sender(user.id(), SenderType.MEMBER, user.nickname());
      } catch (ResponseStatusException ignored) {
        // 토큰이 유효하지 않으면 익명 경로로 넘어간다.
      }
    }

    if (anonymousId == null) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "익명 세션이 필요합니다."
      );
    }

    AnonymousSession session = anonymousSessionRepository
      .findById(anonymousId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "유효하지 않은 익명 세션입니다."
        )
      );

    return new Sender(
      session.getId(),
      SenderType.ANONYMOUS,
      session.getNickname()
    );
  }

  public record Sender(UUID id, SenderType type, String nickname) {}
}
