package com.tech.n.ai.api.contest.dto.response;

import java.util.List;

/**
 * Contest 다건 생성 응답 DTO (내부 API용)
 */
public record ContestBatchResponse(
    Integer totalCount,
    Integer successCount,
    Integer failureCount,
    List<String> failureMessages
) {
    /**
     * Builder를 사용하여 ContestBatchResponse 생성
     */
    public static ContestBatchResponseBuilder builder() {
        return new ContestBatchResponseBuilder();
    }
    
    /**
     * ContestBatchResponse Builder
     */
    public static class ContestBatchResponseBuilder {
        private Integer totalCount;
        private Integer successCount;
        private Integer failureCount;
        private List<String> failureMessages;
        
        public ContestBatchResponseBuilder totalCount(Integer totalCount) {
            this.totalCount = totalCount;
            return this;
        }
        
        public ContestBatchResponseBuilder successCount(Integer successCount) {
            this.successCount = successCount;
            return this;
        }
        
        public ContestBatchResponseBuilder failureCount(Integer failureCount) {
            this.failureCount = failureCount;
            return this;
        }
        
        public ContestBatchResponseBuilder failureMessages(List<String> failureMessages) {
            this.failureMessages = failureMessages;
            return this;
        }
        
        public ContestBatchResponse build() {
            return new ContestBatchResponse(
                totalCount,
                successCount,
                failureCount,
                failureMessages
            );
        }
    }
}
