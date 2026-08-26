package com.khj.mychatback.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        @NotBlank
        @Size(min = 4, max = 20)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "아이디는 영문/숫자/언더스코어만 사용할 수 있습니다.")
        String username,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @NotBlank
        @Size(min = 2, max = 12)
        String nickname,

        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$")
        String phoneNumber,

        // 필수 아님
        @Email
        String email
) {
}
