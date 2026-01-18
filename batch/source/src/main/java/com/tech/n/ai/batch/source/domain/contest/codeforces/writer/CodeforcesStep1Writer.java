package com.tech.n.ai.batch.source.domain.contest.codeforces.writer;

import com.tech.n.ai.api.contest.dto.response.ContestBatchResponse;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

/**
 * Codeforces Step1 Writer
 * batch-source DTO → client-feign DTO 변환 후 내부 API 호출
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class CodeforcesStep1Writer implements ItemWriter<ContestCreateRequest> {

    private final ContestInternalContract contestInternalApi;
    
    @Value("${internal-api.contest.api-key}")
    private String apiKey;

    @Override
    public void write(Chunk<? extends ContestCreateRequest> chunk) throws Exception {
        List<? extends ContestCreateRequest> items = chunk.getItems();
        
        if (items.isEmpty()) {
            log.warn("No items to write");
            return;
        }
        
        // batch-source DTO → client-feign DTO 변환
        List<InternalApiDto.ContestCreateRequest> feignRequests = items.stream()
            .map(item -> {
                InternalApiDto.ContestMetadataRequest metadataRequest = null;
                if (item.metadata() != null) {
                    metadataRequest = InternalApiDto.ContestMetadataRequest.builder()
                        .sourceName(item.metadata().sourceName())
                        .prize(item.metadata().prize())
                        .participants(item.metadata().participants())
                        .tags(item.metadata().tags())
                        .build();
                }
                
                return InternalApiDto.ContestCreateRequest.builder()
                    .sourceId(item.sourceId())
                    .title(item.title())
                    .startDate(item.startDate())
                    .endDate(item.endDate())
                    .description(item.description())
                    .url(item.url())
                    .metadata(metadataRequest)
                    .build();
            })
            .collect(Collectors.toList());
        
        InternalApiDto.ContestBatchRequest batchRequest = InternalApiDto.ContestBatchRequest.builder()
            .contests(feignRequests)
            .build();
        
        // 내부 API 호출 (client-feign DTO 사용)
        ApiResponse<ContestBatchResponse> response = contestInternalApi
            .createContestBatchInternal(apiKey, batchRequest);
        
        // 응답 검증
        if (response == null || !"2000".equals(response.code())) {
            String errorMessage = response != null && response.message() != null 
                ? response.message() 
                : "Unknown error";
            log.error("Failed to create contests batch: {}", errorMessage);
            throw new RuntimeException("Failed to create contests batch: " + errorMessage);
        }
        
        if (response.data() != null) {
            log.info("Successfully created {} contests (total: {}, success: {}, failure: {})", 
                items.size(),
                response.data().totalCount(),
                response.data().successCount(),
                response.data().failureCount());
            
            // 실패 메시지가 있으면 로깅
            if (response.data().failureMessages() != null && !response.data().failureMessages().isEmpty()) {
                log.warn("Some contests failed to create: {}", response.data().failureMessages());
            }
        } else {
            log.warn("Response data is null, but code is 2000");
        }
    }
}
