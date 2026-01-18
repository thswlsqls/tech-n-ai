package com.tech.n.ai.api.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 챗봇 요청 DTO
 */
public record ChatRequest(
    @NotBlank(message = "메시지는 필수입니다.")
    @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다.")
    String message,
    
    String conversationId  // 세션 ID (선택, 없으면 새 세션 생성)
) {}
