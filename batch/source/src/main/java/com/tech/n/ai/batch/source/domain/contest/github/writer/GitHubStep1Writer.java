package com.tech.n.ai.batch.source.domain.contest.github.writer;

import com.ebson.shrimp.tm.demo.api.contest.dto.response.ContestBatchResponse;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.ebson.shrimp.tm.demo.common.core.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class GitHubStep1Writer implements ItemWriter<ContestCreateRequest> {

    private static final String SUCCESS_CODE = "2000";
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    
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
        
        InternalApiDto.ContestBatchRequest batchRequest = convertToBatchRequest(items);
        logBatchRequest(batchRequest);
        
//        ApiResponse<ContestBatchResponse> response = callInternalApi(batchRequest);
//        validateAndLogResponse(response, items.size());
    }

    private InternalApiDto.ContestBatchRequest convertToBatchRequest(List<? extends ContestCreateRequest> items) {
        List<InternalApiDto.ContestCreateRequest> feignRequests = items.stream()
            .map(this::convertToFeignRequest)
            .collect(Collectors.toList());
        
        return InternalApiDto.ContestBatchRequest.builder()
            .contests(feignRequests)
            .build();
    }

    private InternalApiDto.ContestCreateRequest convertToFeignRequest(ContestCreateRequest item) {
        return InternalApiDto.ContestCreateRequest.builder()
            .sourceId(item.sourceId())
            .title(item.title())
            .startDate(item.startDate())
            .endDate(item.endDate())
            .description(item.description())
            .url(item.url())
            .metadata(convertMetadata(item.metadata()))
            .build();
    }

    private InternalApiDto.ContestMetadataRequest convertMetadata(ContestCreateRequest.ContestMetadataRequest metadata) {
        if (metadata == null) {
            return null;
        }
        
        return InternalApiDto.ContestMetadataRequest.builder()
            .sourceName(metadata.sourceName())
            .prize(metadata.prize())
            .participants(metadata.participants())
            .tags(metadata.tags())
            .build();
    }

    private void logBatchRequest(InternalApiDto.ContestBatchRequest batchRequest) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(batchRequest);
            log.info("Batch request size: {}, JSON: {}", batchRequest.getContests().size(), json);
        } catch (Exception e) {
            log.warn("Failed to serialize batch request", e);
        }
    }

    private ApiResponse<ContestBatchResponse> callInternalApi(InternalApiDto.ContestBatchRequest batchRequest) {
        return contestInternalApi.createContestBatchInternal(apiKey, batchRequest);
    }

    private void validateAndLogResponse(ApiResponse<ContestBatchResponse> response, int requestedCount) {
        if (response == null || !SUCCESS_CODE.equals(response.code())) {
            throw new RuntimeException("Failed to create contests batch: " + extractErrorMessage(response));
        }
        
        if (response.data() == null) {
            log.warn("Response data is null");
            return;
        }
        
        logSuccessResult(response.data(), requestedCount);
        logFailureMessages(response.data());
    }

    private String extractErrorMessage(ApiResponse<ContestBatchResponse> response) {
        if (response == null) {
            return "Response is null";
        }
        return response.message() != null ? response.message() : "Unknown error";
    }

    private void logSuccessResult(ContestBatchResponse data, int requestedCount) {
        log.info("Created {} contests - total: {}, success: {}, failure: {}",
            requestedCount,
            data.totalCount(),
            data.successCount(),
            data.failureCount());
    }

    private void logFailureMessages(ContestBatchResponse data) {
        if (data.failureMessages() != null && !data.failureMessages().isEmpty()) {
            log.warn("Failed contests: {}", data.failureMessages());
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
