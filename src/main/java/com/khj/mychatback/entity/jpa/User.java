package com.khj.mychatback.entity.jpa;

import com.khj.mychatback.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 회원 엔티티.
 * - username: 로그인 ID로 사용 (중복 불가)
 * - nickname: 채팅에 노출되는 이름 (중복 불가)
 * - phoneNumber: SMS 인증에 사용된 번호 (중복 불가, 계정당 1개)
 * - email: 선택 입력
 */
@Getter
@Entity
@Table(
  name = "users",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
    @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname"),
    @UniqueConstraint(
      name = "uk_users_phone_number",
      columnNames = "phone_number"
    ),
  }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;

  @Column(nullable = false, length = 50)
  private String username;

  /** BCrypt로 해시된 값만 저장 (평문 저장 금지) */
  @Column(nullable = false)
  private String password;

  @Column(nullable = false, length = 30)
  private String nickname;

  @Column(name = "phone_number", nullable = false, length = 20)
  private String phoneNumber;

  @Column
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Builder
  private User(
    String username,
    String password,
    String nickname,
    String phoneNumber,
    String email,
    Role role
  ) {
    this.username = username;
    this.password = password;
    this.nickname = nickname;
    this.phoneNumber = phoneNumber;
    this.email = email;
    this.role = role != null ? role : Role.USER;
  }

  public void changePassword(String encodedPassword) {
    this.password = encodedPassword;
  }
}
