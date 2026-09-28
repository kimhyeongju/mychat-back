package com.khj.playground.common.security;

import java.util.UUID;

/** 다른 모듈이 알아야 하는 최소한의 회원 정보. */
public record AuthenticatedUser(UUID id, String nickname, boolean admin) {}
