package com.khj.playground.common.security;

import java.util.Optional;
import java.util.UUID;

/** 다른 모듈이 회원 식별자로 최소 정보를 조회하기 위한 계약. auth 모듈이 구현한다. */
public interface MemberLookup {
  Optional<AuthenticatedUser> findById(UUID userId);
}
