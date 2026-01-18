package com.tech.n.ai.api.news.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * News 검색 응답 DTO
 */
public record NewsSearchResponse(
    PageData<NewsDetailResponse> data
) {
    /**
     * PageData로부터 NewsSearchResponse 생성
     * 
     * @param pageData PageData<NewsDetailResponse>
     * @return NewsSearchResponse
     */
    public static NewsSearchResponse from(PageData<NewsDetailResponse> pageData) {
        return new NewsSearchResponse(pageData);
    }
}
