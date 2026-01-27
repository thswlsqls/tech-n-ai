package com.tech.n.ai.datasource.mongodb.enums;

/**
 * 게시물 상태
 */
public enum PostStatus {
    DRAFT,      // 초안 (자동 수집됨)
    PENDING,    // 승인 대기
    PUBLISHED,  // 게시됨
    REJECTED    // 거부됨
}
