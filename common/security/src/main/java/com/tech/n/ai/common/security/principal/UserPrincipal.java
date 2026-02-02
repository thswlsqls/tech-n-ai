package com.tech.n.ai.common.security.principal;

import java.io.Serializable;
import java.security.Principal;

/**
 * JWT 기반 인증 사용자 정보
 * Controller에서 @AuthenticationPrincipal로 주입하여 사용.
 */
public record UserPrincipal(
    Long userId,
    String email,
    String role
) implements Principal, Serializable {

    public static UserPrincipal of(String userId, String email, String role) {
        return new UserPrincipal(Long.parseLong(userId), email, role);
    }

    /** authentication.getName() 하위 호환 */
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
