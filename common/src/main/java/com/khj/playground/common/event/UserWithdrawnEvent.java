package com.khj.playground.common.event;

import java.util.UUID;

/**
 * 회원이 탈퇴(또는 운영자가 삭제)될 때 auth 모듈이 발행한다.
 * 다른 모듈은 이 이벤트를 받아 자기 모듈이 가진 그 회원의 데이터를 정리한다.
 * 같은 트랜잭션 안에서 동기로 처리되므로, 정리에 실패하면 탈퇴 자체가 취소된다.
 */
public record UserWithdrawnEvent(UUID userId) {}
