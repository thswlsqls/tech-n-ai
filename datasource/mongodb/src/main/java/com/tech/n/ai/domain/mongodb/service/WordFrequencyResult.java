package com.tech.n.ai.domain.mongodb.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * 서버사이드 단어 빈도 집계 결과 projection DTO
 * aggregateWordFrequency()의 Aggregation Pipeline에서 $group → $sort → $limit 결과를 매핑
 */
@Getter
@Setter
public class WordFrequencyResult {

    @Id
    private String id;  // 단어 ($group의 _id)
    private long count;
}
