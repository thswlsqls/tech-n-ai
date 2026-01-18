package com.tech.n.ai.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 확인 요청 DTO
 */
public record ResetPasswordConfirmRequest(
    @NotBlank(message = "토큰은 필수입니다.")
    String token,
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])|(?=.*[a-zA-Z])(?=.*[^a-zA-Z0-9])|(?=.*[0-9])(?=.*[^a-zA-Z0-9]).*$", 
             message = "비밀번호는 대소문자/숫자/특수문자 중 2가지 이상을 포함해야 합니다.")
    String newPassword
) {
}
