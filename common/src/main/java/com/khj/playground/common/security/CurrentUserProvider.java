package com.khj.playground.common.security;

import org.springframework.security.core.Authentication;

/**
 * auth 모듈이 구현한다. 다른 모듈은 User 엔티티를 몰라도 된다.
 */
public interface CurrentUserProvider {
  AuthenticatedUser resolve(Authentication authentication);
}
