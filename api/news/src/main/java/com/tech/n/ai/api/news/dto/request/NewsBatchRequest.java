package com.tech.n.ai.api.news.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * News 다건 생성 요청 DTO (내부 API용)
 */
public record NewsBatchRequest(
    @NotEmpty(message = "News 목록은 필수입니다.")
    @Valid
    List<NewsCreateRequest> newsArticles
) {
}
