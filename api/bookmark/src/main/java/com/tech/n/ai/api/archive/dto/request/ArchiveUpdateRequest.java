package com.tech.n.ai.api.archive.dto.request;

/**
 * 아카이브 수정 요청 DTO
 */
public record ArchiveUpdateRequest(
    String tag,
    String memo
) {
}
