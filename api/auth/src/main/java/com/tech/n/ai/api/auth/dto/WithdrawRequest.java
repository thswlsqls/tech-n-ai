package com.tech.n.ai.api.auth.dto;

import jakarta.validation.constraints.Size;

/**
 * 회원탈퇴 요청 DTO
 */
public record WithdrawRequest(
    /**
     * 비밀번호 확인 (선택적)
     * 보안 강화를 위해 비밀번호 재확인을 요구할 수 있음
     */
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
    String password,
    
    /**
     * 탈퇴 사유 (선택적)
     * 사용자 피드백 수집 목적
     */
    @Size(max = 500, message = "탈퇴 사유는 500자 이하여야 합니다.")
    String reason
) {
}
