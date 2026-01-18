package com.tech.n.ai.client.scraper.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 스크래핑된 대회 정보 DTO
 * 웹 스크래핑을 통해 수집된 대회 정보를 나타냄
 */
@Builder
public record ScrapedContestItem(
        /**
         * 대회 제목
         */
        String title,
        
        /**
         * 대회 URL
         */
        String url,
        
        /**
         * 대회 설명
         */
        String description,
        
        /**
         * 대회 시작일시
         */
        LocalDateTime startDate,
        
        /**
         * 대회 종료일시
         */
        LocalDateTime endDate,
        
        /**
         * 대회 주최자/조직
         */
        String organizer,
        
        /**
         * 대회 장소 (온라인/오프라인)
         */
        String location,
        
        /**
         * 대회 카테고리/태그
         */
        String category,
        
        /**
         * 상금/보상 정보
         */
        String prize,
        
        /**
         * 대회 이미지 URL
         */
        String imageUrl
) {
}
