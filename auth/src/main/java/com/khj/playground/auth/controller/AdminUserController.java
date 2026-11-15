package com.khj.playground.auth.controller;

import com.khj.playground.auth.dto.AdminCreateUserRequest;
import com.khj.playground.auth.dto.AdminUpdateUserRequest;
import com.khj.playground.auth.dto.AdminUserResponse;
import com.khj.playground.auth.service.AdminUserService;
import com.khj.playground.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 운영자 전용 회원 관리. SecurityConfig 가 /api/admin/** 를 ADMIN 권한으로만 연다. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final AdminUserService adminUserService;

  @GetMapping
  public PageResponse<AdminUserResponse> list(
    @RequestParam(name = "q", required = false) String keyword,
    @RequestParam(name = "page", defaultValue = "0") int page,
    @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    return adminUserService.list(keyword, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AdminUserResponse create(
    @Valid @RequestBody AdminCreateUserRequest request,
    Authentication authentication
  ) {
    return adminUserService.create(request, authentication.getName());
  }

  @PatchMapping("/{id}")
  public AdminUserResponse update(
    @PathVariable("id") UUID id,
    @Valid @RequestBody AdminUpdateUserRequest request,
    Authentication authentication
  ) {
    return adminUserService.update(id, request, authentication.getName());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") UUID id, Authentication authentication) {
    adminUserService.delete(id, authentication.getName());
  }
}
