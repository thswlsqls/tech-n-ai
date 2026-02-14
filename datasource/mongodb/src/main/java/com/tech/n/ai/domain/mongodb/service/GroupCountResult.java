package com.tech.n.ai.domain.mongodb.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * 그룹별 집계 결과 projection DTO
 */
@Getter
@Setter
public class GroupCountResult {

    @Id
    private String id;  // 그룹 필드 값 (Aggregation $group의 _id)
    private long count;
}
