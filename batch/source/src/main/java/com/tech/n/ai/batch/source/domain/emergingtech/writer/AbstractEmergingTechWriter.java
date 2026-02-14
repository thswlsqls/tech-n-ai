package com.tech.n.ai.batch.source.domain.emergingtech.writer;

import com.tech.n.ai.batch.source.domain.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Emerging Tech Internal API Writer 공통 추상 클래스 (Template Method 패턴)
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEmergingTechWriter implements ItemWriter<EmergingTechCreateRequest> {

    private static final String SUCCESS_CODE = "2000";
    private static final double NULL_RATIO_WARN_THRESHOLD = 0.5;
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private final EmergingTechInternalContract emergingTechInternalApi;

    @Value("${internal-api.emerging-tech.api-key}")
    private String apiKey;

    /**
     * 소스 타입명 (로그 출력용)
     */
    protected abstract String getSourceType();

    /**
     * 배치 잡 이름 (리포트 로그용)
     */
    protected abstract String getJobName();

    @Override
    public void write(Chunk<? extends EmergingTechCreateRequest> chunk) throws Exception {
        List<? extends EmergingTechCreateRequest> items = chunk.getItems();

        if (items.isEmpty()) {
            log.warn("No {} items to write", getSourceType());
            return;
        }

        logPublishedAtNullRatio(items);

        InternalApiDto.EmergingTechBatchRequest batchRequest = convertToBatchRequest(items);
        logBatchRequest(batchRequest);

        ApiResponse<InternalApiDto.EmergingTechBatchResponse> response =
            emergingTechInternalApi.createEmergingTechBatchInternal(apiKey, batchRequest);
        validateAndLogResponse(response, items.size());
    }

    private void logPublishedAtNullRatio(List<? extends EmergingTechCreateRequest> items) {
        Map<String, int[]> providerStats = new LinkedHashMap<>();

        for (EmergingTechCreateRequest item : items) {
            String provider = item.provider() != null ? item.provider() : "UNKNOWN";
            int[] counts = providerStats.computeIfAbsent(provider, k -> new int[2]);
            counts[0]++;
            if (item.publishedAt() == null) {
                counts[1]++;
            }
        }

        StringBuilder report = new StringBuilder();
        report.append(String.format("[Batch Report] %s publishedAt null ratio%n", getJobName()));

        boolean hasHighNullRatio = false;

        for (Map.Entry<String, int[]> entry : providerStats.entrySet()) {
            String provider = entry.getKey();
            int total = entry.getValue()[0];
            int nullCount = entry.getValue()[1];
            double ratio = total > 0 ? (double) nullCount / total : 0;
            String percentage = String.format("%.1f%%", ratio * 100);

            report.append(String.format("  ├─ %-12s: %d articles, %d null publishedAt (%s)%n",
                    provider, total, nullCount, percentage));

            if (ratio >= NULL_RATIO_WARN_THRESHOLD) {
                hasHighNullRatio = true;
            }
        }

        if (hasHighNullRatio) {
            log.warn(report.toString());
        } else {
            log.info(report.toString());
        }
    }

    private InternalApiDto.EmergingTechBatchRequest convertToBatchRequest(List<? extends EmergingTechCreateRequest> items) {
        List<InternalApiDto.EmergingTechCreateRequest> feignRequests = items.stream()
            .map(this::convertToFeignRequest)
            .collect(Collectors.toList());

        return InternalApiDto.EmergingTechBatchRequest.builder()
            .items(feignRequests)
            .build();
    }

    private InternalApiDto.EmergingTechCreateRequest convertToFeignRequest(EmergingTechCreateRequest item) {
        InternalApiDto.EmergingTechMetadataRequest metadata = null;
        if (item.metadata() != null) {
            metadata = InternalApiDto.EmergingTechMetadataRequest.builder()
                .version(item.metadata().version())
                .tags(item.metadata().tags())
                .author(item.metadata().author())
                .githubRepo(item.metadata().githubRepo())
                .build();
        }

        return InternalApiDto.EmergingTechCreateRequest.builder()
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

    private void logBatchRequest(InternalApiDto.EmergingTechBatchRequest batchRequest) {
        log.info("Batch request size: {}", batchRequest.getItems().size());

        try {
            String json = OBJECT_MAPPER.writeValueAsString(batchRequest);
            log.info("Batch request JSON: {}", json);
        } catch (Exception e) {
            log.warn("Failed to serialize batch request", e);
        }
    }

    private void validateAndLogResponse(ApiResponse<InternalApiDto.EmergingTechBatchResponse> response, int requestedCount) {
        if (response == null) {
            throw new RuntimeException(
                String.format("Emerging Tech %s batch creation failed: Response is null", getSourceType()));
        }

        if (!SUCCESS_CODE.equals(response.code())) {
            throw new RuntimeException(
                String.format("Emerging Tech %s batch creation failed: %s", getSourceType(), response.message()));
        }

        InternalApiDto.EmergingTechBatchResponse data = response.data();
        if (data != null) {
            log.info("Internal API 응답: total={}, new={}, duplicate={}, failure={}",
                data.getTotalCount(), data.getNewCount(), data.getDuplicateCount(), data.getFailureCount());
            if (data.getFailureCount() > 0) {
                log.warn("Internal API 실패 항목: {}", data.getFailureMessages());
            }
        }

        log.info("Emerging Tech {} batch creation completed: requested={}", getSourceType(), requestedCount);
    }

    private static ObjectMapper createObjectMapper() {
        // Jackson 3: ObjectMapper is immutable, use JsonMapper.builder()
        // Jackson 3 defaults: WRITE_DATES_AS_TIMESTAMPS=false (ISO-8601)
        return JsonMapper.builder().build();
    }
}
