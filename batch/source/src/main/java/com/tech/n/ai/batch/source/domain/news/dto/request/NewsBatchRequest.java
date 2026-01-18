package com.tech.n.ai.batch.source.domain.news.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Builder;

/**
 * News 다건 생성 요청 DTO (내부 API용)
 * api-news 모듈의 DTO와 필드가 같아도 별도 정의
 */
@Builder
public record NewsBatchRequest(
    @NotEmpty(message = "News 목록은 필수입니다.")
    @Valid
    List<NewsCreateRequest> newsArticles
) {
}
