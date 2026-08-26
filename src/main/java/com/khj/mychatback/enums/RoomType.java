package com.khj.mychatback.enums;

public enum RoomType {
  /** 위치 기반 익명 채팅방 (중심 좌표 + 반경) */
  LOCATION,
  /** 회원간 1:1 DM (인증 필수) */
  DIRECT,
  /** 자유 생성 공개 채팅방 (추후 기능) */
  PUBLIC,
}
