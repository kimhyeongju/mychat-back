package com.khj.mychatback.api.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.khj.mychatback.api.auth.dto.LoginRequest;
import com.khj.mychatback.api.auth.dto.PhoneVerificationConfirmRequest;
import com.khj.mychatback.api.auth.dto.PhoneVerificationSendRequest;
import com.khj.mychatback.api.auth.dto.SignUpRequest;
import com.khj.mychatback.api.auth.dto.SignUpResponse;
import com.khj.mychatback.api.auth.dto.TokenRefreshRequest;
import com.khj.mychatback.api.auth.dto.TokenResponse;
import com.khj.mychatback.api.auth.service.AuthService;
import com.khj.mychatback.api.auth.service.PhoneVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/phone/send-code")
    public void sendPhoneVerificationCode(@Valid @RequestBody PhoneVerificationSendRequest request) {
        phoneVerificationService.sendVerificationCode(request.phoneNumber());
    }

    @PostMapping("/phone/verify")
    public void verifyPhoneCode(@Valid @RequestBody PhoneVerificationConfirmRequest request) {
        boolean verified = phoneVerificationService.verifyCode(request.phoneNumber(), request.code());
        if (!verified) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않거나 만료되었습니다.");
        }
    }

    @PostMapping("/signup")
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/reissue")
    public TokenResponse reissue(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.reissue(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(Authentication authentication) {
        authService.logout(authentication.getName());
    }
}
