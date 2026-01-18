package com.tech.n.ai.client.rss.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * RSS 피드 아이템 DTO
 * 파싱된 RSS 피드의 개별 아이템을 나타냄
 */
@Builder
public record RssFeedItem(
        /**
         * 아이템 제목
         */
        String title,
        
        /**
         * 아이템 링크 URL
         */
        String link,
        
        /**
         * 아이템 설명/내용
         */
        String description,
        
        /**
         * 발행일시
         */
        LocalDateTime publishedDate,
        
        /**
         * 작성자
         */
        String author,
        
        /**
         * 카테고리/태그
         */
        String category,
        
        /**
         * GUID (고유 식별자)
         */
        String guid,
        
        /**
         * 이미지 URL
         */
        String imageUrl
) {
}
