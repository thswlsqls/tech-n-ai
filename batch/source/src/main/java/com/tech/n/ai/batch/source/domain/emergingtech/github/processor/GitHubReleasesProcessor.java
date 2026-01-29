package com.tech.n.ai.batch.source.domain.emergingtech.github.processor;

import com.tech.n.ai.batch.source.domain.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.batch.source.domain.emergingtech.github.reader.GitHubReleaseWithRepo;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import com.tech.n.ai.domain.mongodb.enums.EmergingTechType;
import com.tech.n.ai.domain.mongodb.enums.PostStatus;
import com.tech.n.ai.domain.mongodb.enums.SourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GitHub Release → EmergingTechCreateRequest 변환 Processor
 */
@Slf4j
@StepScope
public class GitHubReleasesProcessor implements ItemProcessor<GitHubReleaseWithRepo, EmergingTechCreateRequest> {

    @Override
    public @Nullable EmergingTechCreateRequest process(GitHubReleaseWithRepo item) throws Exception {
        if (!isValidItem(item)) {
            return null;
        }

        return buildEmergingTechCreateRequest(item);
    }

    private boolean isValidItem(GitHubReleaseWithRepo item) {
        if (item == null || item.release() == null) {
            log.warn("Release is null");
            return false;
        }

        GitHubDto.Release release = item.release();

        // prerelease, draft 제외
        if (Boolean.TRUE.equals(release.prerelease())) {
            log.debug("Skipping prerelease: {}", release.tagName());
            return false;
        }

        if (Boolean.TRUE.equals(release.draft())) {
            log.debug("Skipping draft: {}", release.tagName());
            return false;
        }

        return true;
    }

    private EmergingTechCreateRequest buildEmergingTechCreateRequest(GitHubReleaseWithRepo item) {
        GitHubDto.Release release = item.release();

        return EmergingTechCreateRequest.builder()
            .provider(item.provider())
            .updateType(EmergingTechType.SDK_RELEASE.name())
            .title(buildTitle(release))
            .summary(truncate(release.body(), 500))
            .url(release.htmlUrl())
            .publishedAt(parsePublishedAt(release.publishedAt()))
            .sourceType(SourceType.GITHUB_RELEASE.name())
            .status(PostStatus.DRAFT.name())
            .externalId("github:" + release.id())
            .metadata(buildMetadata(item))
            .build();
    }

    private String buildTitle(GitHubDto.Release release) {
        if (release.name() != null && !release.name().isBlank()) {
            return release.name();
        }
        return release.tagName();
    }

    private EmergingTechCreateRequest.EmergingTechMetadataRequest buildMetadata(GitHubReleaseWithRepo item) {
        GitHubDto.Release release = item.release();
        String author = release.author() != null ? release.author().login() : null;

        return EmergingTechCreateRequest.EmergingTechMetadataRequest.builder()
            .version(release.tagName())
            .tags(List.of("sdk", "release"))
            .author(author)
            .githubRepo(item.owner() + "/" + item.repo())
            .build();
    }

    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse publishedAt: {}", publishedAt);
            return LocalDateTime.now();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
