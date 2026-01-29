package com.tech.n.ai.batch.source.domain.emergingtech.github.reader;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import lombok.Builder;

/**
 * GitHub Release + Repository 정보
 */
@Builder
public record GitHubReleaseWithRepo(
    GitHubDto.Release release,
    String owner,
    String repo,
    String provider
) {}
