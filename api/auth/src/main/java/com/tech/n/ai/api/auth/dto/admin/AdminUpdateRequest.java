package com.tech.n.ai.api.auth.dto.admin;

import jakarta.validation.constraints.Size;

public record AdminUpdateRequest(
    @Size(min = 2, max = 50, message = "사용자명은 2-50자 사이여야 합니다.")
    String username,

    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    String password
) {}
