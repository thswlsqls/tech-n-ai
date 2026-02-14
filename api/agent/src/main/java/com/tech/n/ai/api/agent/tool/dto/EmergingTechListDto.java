package com.tech.n.ai.api.agent.tool.dto;

import java.util.List;

/**
 * 목록 조회 결과 DTO
 * LangChain4j @Tool 반환 시 JSON 직렬화되어 LLM에 전달됨
 */
public record EmergingTechListDto(
    int totalCount,
    int page,
    int size,
    int totalPages,
    String period,              // 조회 기간 (예: "2026-01-01 ~ 2026-01-31" 또는 "전체")
    List<EmergingTechDto> items // 기존 EmergingTechDto 재사용
) {
    public static EmergingTechListDto empty(int page, int size, String period) {
        return new EmergingTechListDto(0, page, size, 0, period, List.of());
    }
}
