package com.tech.n.ai.batch.source.domain.news.techcrunch.writer;

import com.ebson.shrimp.tm.demo.api.news.dto.response.NewsBatchResponse;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.ebson.shrimp.tm.demo.common.core.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class TechCrunchStep1Writer implements ItemWriter<NewsCreateRequest> {

    private static final String SUCCESS_CODE = "2000";
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

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

        InternalApiDto.NewsBatchRequest batchRequest = convertToBatchRequest(items);
        logBatchRequest(batchRequest);

        ApiResponse<NewsBatchResponse> response = callInternalApi(batchRequest);
        validateAndLogResponse(response, items.size());
    }

    private InternalApiDto.NewsBatchRequest convertToBatchRequest(List<? extends NewsCreateRequest> items) {
        List<InternalApiDto.NewsCreateRequest> feignRequests = items.stream()
            .map(this::convertToFeignRequest)
            .collect(Collectors.toList());

        return InternalApiDto.NewsBatchRequest.builder()
            .newsArticles(feignRequests)
            .build();
    }

    private InternalApiDto.NewsCreateRequest convertToFeignRequest(NewsCreateRequest item) {
        return InternalApiDto.NewsCreateRequest.builder()
            .sourceId(item.sourceId())
            .title(item.title())
            .content(item.content())
            .summary(item.summary())
            .publishedAt(item.publishedAt())
            .url(item.url())
            .author(item.author())
            .metadata(convertMetadata(item.metadata()))
            .build();
    }

    private InternalApiDto.NewsMetadataRequest convertMetadata(NewsCreateRequest.NewsMetadataRequest metadata) {
        if (metadata == null) {
            return null;
        }

        return InternalApiDto.NewsMetadataRequest.builder()
            .sourceName(metadata.sourceName())
            .tags(metadata.tags())
            .viewCount(metadata.viewCount())
            .likeCount(metadata.likeCount())
            .build();
    }

    private void logBatchRequest(InternalApiDto.NewsBatchRequest batchRequest) {
        log.info("Batch request size: {}", batchRequest.getNewsArticles().size());

        try {
            String json = OBJECT_MAPPER.writeValueAsString(batchRequest);
            log.info("Batch request JSON: {}", json);
        } catch (Exception e) {
            log.warn("Failed to serialize batch request", e);
        }
    }

    private ApiResponse<NewsBatchResponse> callInternalApi(InternalApiDto.NewsBatchRequest batchRequest) {
        return newsInternalApi.createNewsBatchInternal(apiKey, batchRequest);
    }

    private void validateAndLogResponse(ApiResponse<NewsBatchResponse> response, int requestedCount) {
        if (response == null || !SUCCESS_CODE.equals(response.code())) {
            throw new RuntimeException("Failed to create news batch: " + extractErrorMessage(response));
        }

        if (response.data() == null) {
            log.warn("Response data is null");
            return;
        }

        logSuccessResult(response.data(), requestedCount);
        logFailureMessages(response.data());
    }

    private String extractErrorMessage(ApiResponse<NewsBatchResponse> response) {
        if (response == null) {
            return "Response is null";
        }
        return response.message() != null ? response.message() : "Unknown error";
    }

    private void logSuccessResult(NewsBatchResponse data, int requestedCount) {
        log.info("Created {} articles - total: {}, success: {}, failure: {}",
            requestedCount,
            data.totalCount(),
            data.successCount(),
            data.failureCount());
    }

    private void logFailureMessages(NewsBatchResponse data) {
        if (data.failureMessages() != null && !data.failureMessages().isEmpty()) {
            log.warn("Failed articles: {}", data.failureMessages());
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
