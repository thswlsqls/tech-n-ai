package com.tech.n.ai.batch.source.domain.emergingtech.github.service;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * GitHub Releases API 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubReleasesService {

    private final GitHubContract githubContract;

    /**
     * GitHub Releases 조회
     */
    public List<GitHubDto.Release> getReleases(String owner, String repo, int perPage, int page) {
        log.debug("GitHub Releases 조회: owner={}, repo={}, perPage={}, page={}", owner, repo, perPage, page);

        GitHubDto.ReleasesRequest request = GitHubDto.ReleasesRequest.builder()
            .owner(owner)
            .repo(repo)
            .perPage(perPage)
            .page(page)
            .build();

        GitHubDto.ReleasesResponse response = githubContract.getReleases(request);

        if (response == null || response.releases() == null) {
            return List.of();
        }

        log.info("GitHub Releases 조회 완료: owner={}, repo={}, count={}", owner, repo, response.releases().size());
        return response.releases();
    }
}
