package com.tech.n.ai.batch.source.domain.news.googledevelopers.writer;

import com.tech.n.ai.api.news.dto.response.NewsBatchResponse;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
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
 * Google Developers Step1 Writer
 * batch-source DTO → client-feign DTO 변환 후 내부 API 호출
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class GoogleDevelopersStep1Writer implements ItemWriter<NewsCreateRequest> {

    private final NewsInternalContract newsInternalApi;
    
    @Value("${internal-api.news.api-key}")
    private String apiKey;

    @Override
    public void write(Chunk<? extends NewsCreateRequest> chunk) throws Exception {
        List<? extends NewsCreateRequest> items = chunk.getItems();
        
        if (items.isEmpty()) {
            log.warn("No items to write");
            return;
        }
        
        // batch-source DTO → client-feign DTO 변환
        List<InternalApiDto.NewsCreateRequest> feignRequests = items.stream()
            .map(item -> {
                InternalApiDto.NewsMetadataRequest metadataRequest = null;
                if (item.metadata() != null) {
                    metadataRequest = InternalApiDto.NewsMetadataRequest.builder()
                        .sourceName(item.metadata().sourceName())
                        .tags(item.metadata().tags())
                        .viewCount(item.metadata().viewCount())
                        .likeCount(item.metadata().likeCount())
                        .build();
                }
                
                return InternalApiDto.NewsCreateRequest.builder()
                    .sourceId(item.sourceId())
                    .title(item.title())
                    .content(item.content())
                    .summary(item.summary())
                    .publishedAt(item.publishedAt())
                    .url(item.url())
                    .author(item.author())
                    .metadata(metadataRequest)
                    .build();
            })
            .collect(Collectors.toList());
        
        InternalApiDto.NewsBatchRequest batchRequest = InternalApiDto.NewsBatchRequest.builder()
            .newsArticles(feignRequests)
            .build();
        
        // 내부 API 호출 (client-feign DTO 사용)
        ApiResponse<NewsBatchResponse> response = newsInternalApi
            .createNewsBatchInternal(apiKey, batchRequest);
        
        // 응답 검증
        if (response == null || !"2000".equals(response.code())) {
            String errorMessage = response != null && response.message() != null 
                ? response.message() 
                : "Unknown error";
            log.error("Failed to create news batch: {}", errorMessage);
            throw new RuntimeException("Failed to create news batch: " + errorMessage);
        }
        
        if (response.data() != null) {
            log.info("Successfully created {} news articles (total: {}, success: {}, failure: {})", 
                items.size(),
                response.data().totalCount(),
                response.data().successCount(),
                response.data().failureCount());
            
            // 실패 메시지가 있으면 로깅
            if (response.data().failureMessages() != null && !response.data().failureMessages().isEmpty()) {
                log.warn("Some news articles failed to create: {}", response.data().failureMessages());
            }
        } else {
            log.warn("Response data is null, but code is 2000");
        }
    }
}
