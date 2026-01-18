package com.tech.n.ai.api.news.dto.response;

import com.tech.n.ai.common.core.dto.PageData;

/**
 * News 목록 조회 응답 DTO
 */
public record NewsListResponse(
    PageData<NewsDetailResponse> data
) {
    /**
     * PageData로부터 NewsListResponse 생성
     * 
     * @param pageData PageData<NewsDetailResponse>
     * @return NewsListResponse
     */
    public static NewsListResponse from(PageData<NewsDetailResponse> pageData) {
        return new NewsListResponse(pageData);
    }
}
