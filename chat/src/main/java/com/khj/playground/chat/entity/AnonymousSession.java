package com.khj.playground.chat.entity;

import com.khj.playground.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 익명 사용자의 신원. id가 곧 클라이언트가 보관하는 세션 토큰이다.
 * 권한을 전혀 갖지 않으며, 같은 사람이 보낸 메시지를 묶는 용도로만 쓴다.
 */
@Getter
@Entity
@Table(name = "chat_anonymous_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnonymousSession extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @Column(nullable = false, length = 30)
  private String nickname;

  public static AnonymousSession create(String nickname) {
    AnonymousSession session = new AnonymousSession();
    session.nickname = nickname;
    return session;
  }
}
