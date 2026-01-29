package com.tech.n.ai.batch.source.domain.emergingtech.rss.processor;

import com.tech.n.ai.batch.source.domain.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.domain.mongodb.enums.EmergingTechType;
import com.tech.n.ai.domain.mongodb.enums.PostStatus;
import com.tech.n.ai.domain.mongodb.enums.SourceType;
import com.tech.n.ai.domain.mongodb.enums.TechProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * RSS FeedItem → EmergingTechCreateRequest 변환 Processor
 */
@Slf4j
@StepScope
public class EmergingTechRssProcessor implements ItemProcessor<RssFeedItem, EmergingTechCreateRequest> {

    @Override
    public @Nullable EmergingTechCreateRequest process(RssFeedItem item) throws Exception {
        if (item.title() == null || item.link() == null) {
            log.debug("Skipping RSS item: title or link is null");
            return null;
        }

        if (item.publishedDate() != null && item.publishedDate().isBefore(LocalDateTime.now().minusMonths(1))) {
            log.debug("Skipping RSS item: publishedAt is older than 1 month - title={}, publishedAt={}", item.title(), item.publishedDate());
            return null;
        }

        TechProvider provider = resolveProvider(item.link());

        return EmergingTechCreateRequest.builder()
            .provider(provider.name())
            .updateType(classifyUpdateType(item).name())
            .title(item.title())
            .summary(truncateHtml(item.description(), 500))
            .url(item.link())
            .publishedAt(item.publishedDate())
            .sourceType(SourceType.RSS.name())
            .status(PostStatus.DRAFT.name())
            .externalId("rss:" + generateHash(item.guid() != null ? item.guid() : item.link()))
            .metadata(EmergingTechCreateRequest.EmergingTechMetadataRequest.builder()
                .tags(extractTags(item.category()))
                .author(Objects.requireNonNullElse(item.author(), ""))
                .build())
            .build();
    }

    private TechProvider resolveProvider(String url) {
        if (url.contains("openai.com")) return TechProvider.OPENAI;
        if (url.contains("blog.google")) return TechProvider.GOOGLE;
        throw new IllegalArgumentException("Unknown RSS source URL: " + url);
    }

    private EmergingTechType classifyUpdateType(RssFeedItem item) {
        String title = item.title().toLowerCase();
        String category = item.category() != null ? item.category().toLowerCase() : "";

        if (title.contains("api") || category.contains("api")) return EmergingTechType.API_UPDATE;
        if (title.contains("release") || title.contains("introducing") || title.contains("model")
                || title.contains("announcing")) return EmergingTechType.MODEL_RELEASE;
        if (title.contains("launch") || title.contains("product") || title.contains("available")
                || title.contains("new service")) return EmergingTechType.PRODUCT_LAUNCH;
        if (title.contains("platform") || title.contains("cloud") || title.contains("infrastructure")
                || title.contains("update") || category.contains("platform")) return EmergingTechType.PLATFORM_UPDATE;
        return EmergingTechType.BLOG_POST;
    }

    private String truncateHtml(String text, int maxLength) {
        if (text == null) return "";
        // HTML 태그 제거
        String cleaned = text.replaceAll("<[^>]+>", "").trim();
        if (cleaned.length() <= maxLength) return cleaned;
        return cleaned.substring(0, maxLength) + "...";
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private List<String> extractTags(String category) {
        if (category == null || category.isBlank()) return List.of();
        return List.of(category.split(",")).stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
