package com.tech.n.ai.batch.source.domain.contest.dto.response;

import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contest 상세 조회 응답 DTO
 */
public record ContestDetailResponse(
    String id,
    String sourceId,
    String title,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String status,
    String description,
    String url,
    ContestMetadataResponse metadata,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    /**
     * ContestDocument로부터 ContestDetailResponse 생성
     * 
     * @param document ContestDocument
     * @return ContestDetailResponse
     */
    public static ContestDetailResponse from(ContestDocument document) {
        if (document == null) {
            return null;
        }
        
        ContestMetadataResponse metadataResponse = null;
        if (document.getMetadata() != null) {
            metadataResponse = new ContestMetadataResponse(
                document.getMetadata().getSourceName(),
                document.getMetadata().getPrize(),
                document.getMetadata().getParticipants(),
                document.getMetadata().getTags()
            );
        }
        
        return new ContestDetailResponse(
            document.getId() != null ? document.getId().toHexString() : null,
            document.getSourceId() != null ? document.getSourceId().toHexString() : null,
            document.getTitle(),
            document.getStartDate(),
            document.getEndDate(),
            document.getStatus(),
            document.getDescription(),
            document.getUrl(),
            metadataResponse,
            document.getCreatedAt(),
            document.getCreatedBy(),
            document.getUpdatedAt(),
            document.getUpdatedBy()
        );
    }
    
    /**
     * ContestMetadata 응답 DTO
     */
    public record ContestMetadataResponse(
        String sourceName,
        String prize,
        Integer participants,
        List<String> tags
    ) {
    }
}
