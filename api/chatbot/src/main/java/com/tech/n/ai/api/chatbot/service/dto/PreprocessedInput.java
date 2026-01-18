package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * 전처리된 입력 데이터
 */
@Builder
public record PreprocessedInput(
    String original,
    String normalized,
    String cleaned,
    Integer length
) {}
