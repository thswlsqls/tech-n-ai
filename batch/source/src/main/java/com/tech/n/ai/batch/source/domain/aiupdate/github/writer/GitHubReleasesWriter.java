package com.tech.n.ai.batch.source.domain.aiupdate.github.writer;

import com.tech.n.ai.batch.source.domain.aiupdate.dto.request.AiUpdateCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.AiUpdateInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GitHub Releases → AI Update Internal API Writer
 */
@Slf4j
@StepScope
@RequiredArgsConstructor
public class GitHubReleasesWriter implements ItemWriter<AiUpdateCreateRequest> {

    private static final String SUCCESS_CODE = "2000";

    private final AiUpdateInternalContract aiUpdateInternalApi;

    @Value("${internal-api.ai-update.api-key}")
    private String apiKey;

    @Override
    public void write(Chunk<? extends AiUpdateCreateRequest> chunk) throws Exception {
        List<? extends AiUpdateCreateRequest> items = chunk.getItems();

        if (items.isEmpty()) {
            log.warn("No items to write");
            return;
        }

        InternalApiDto.AiUpdateBatchRequest batchRequest = convertToBatchRequest(items);
        log.info("Sending batch request: size={}", batchRequest.getItems().size());

        ApiResponse<Object> response = callInternalApi(batchRequest);
        validateAndLogResponse(response, items.size());
    }

    private InternalApiDto.AiUpdateBatchRequest convertToBatchRequest(List<? extends AiUpdateCreateRequest> items) {
        List<InternalApiDto.AiUpdateCreateRequest> feignRequests = items.stream()
            .map(this::convertToFeignRequest)
            .collect(Collectors.toList());

        return InternalApiDto.AiUpdateBatchRequest.builder()
            .items(feignRequests)
            .build();
    }

    private InternalApiDto.AiUpdateCreateRequest convertToFeignRequest(AiUpdateCreateRequest item) {
        InternalApiDto.AiUpdateMetadataRequest metadata = null;
        if (item.metadata() != null) {
            metadata = InternalApiDto.AiUpdateMetadataRequest.builder()
                .version(item.metadata().version())
                .tags(item.metadata().tags())
                .author(item.metadata().author())
                .githubRepo(item.metadata().githubRepo())
                .build();
        }

        return InternalApiDto.AiUpdateCreateRequest.builder()
            .provider(item.provider())
            .updateType(item.updateType())
            .title(item.title())
            .summary(item.summary())
            .url(item.url())
            .publishedAt(item.publishedAt())
            .sourceType(item.sourceType())
            .status(item.status())
            .externalId(item.externalId())
            .metadata(metadata)
            .build();
    }

    private ApiResponse<Object> callInternalApi(InternalApiDto.AiUpdateBatchRequest batchRequest) {
        return aiUpdateInternalApi.createAiUpdateBatchInternal(apiKey, batchRequest);
    }

    private void validateAndLogResponse(ApiResponse<Object> response, int requestedCount) {
        if (response == null) {
            throw new RuntimeException("AI Update batch creation failed: Response is null");
        }

        if (!SUCCESS_CODE.equals(response.code())) {
            throw new RuntimeException("AI Update batch creation failed: " + response.message());
        }

        log.info("AI Update batch creation completed: requested={}", requestedCount);
    }
}
