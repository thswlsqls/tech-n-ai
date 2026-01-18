package com.tech.n.ai.api.news.dto.response;

import java.util.List;

/**
 * News 다건 생성 응답 DTO (내부 API용)
 */
public record NewsBatchResponse(
    Integer totalCount,
    Integer successCount,
    Integer failureCount,
    List<String> failureMessages
) {
    /**
     * Builder를 사용하여 NewsBatchResponse 생성
     */
    public static NewsBatchResponseBuilder builder() {
        return new NewsBatchResponseBuilder();
    }
    
    /**
     * NewsBatchResponse Builder
     */
    public static class NewsBatchResponseBuilder {
        private Integer totalCount;
        private Integer successCount;
        private Integer failureCount;
        private List<String> failureMessages;
        
        public NewsBatchResponseBuilder totalCount(Integer totalCount) {
            this.totalCount = totalCount;
            return this;
        }
        
        public NewsBatchResponseBuilder successCount(Integer successCount) {
            this.successCount = successCount;
            return this;
        }
        
        public NewsBatchResponseBuilder failureCount(Integer failureCount) {
            this.failureCount = failureCount;
            return this;
        }
        
        public NewsBatchResponseBuilder failureMessages(List<String> failureMessages) {
            this.failureMessages = failureMessages;
            return this;
        }
        
        public NewsBatchResponse build() {
            return new NewsBatchResponse(
                totalCount,
                successCount,
                failureCount,
                failureMessages
            );
        }
    }
}
