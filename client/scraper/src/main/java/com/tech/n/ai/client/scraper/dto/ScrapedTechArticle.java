package com.tech.n.ai.client.scraper.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 기술 블로그 스크래핑 결과 DTO
 */
@Builder
public record ScrapedTechArticle(
        String title,
        String url,
        String summary,
        LocalDateTime publishedDate,
        String author,
        String category,
        String providerName
) {}
