package com.tech.n.ai.api.agent.service;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent용 GitHub 릴리스 수집 서비스
 * GitHubContract를 통해 릴리스를 조회하고 draft/prerelease를 필터링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubDataCollectionService {

    private final GitHubContract githubContract;

    /**
     * GitHub 저장소의 유효한 릴리스 조회
     *
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     * @param perPage 조회 건수
     * @return draft/prerelease가 제외된 릴리스 목록
     */
    public List<GitHubDto.Release> fetchValidReleases(String owner, String repo, int perPage) {
        try {
            GitHubDto.ReleasesRequest request = GitHubDto.ReleasesRequest.builder()
                .owner(owner)
                .repo(repo)
                .perPage(perPage)
                .page(1)
                .build();

            GitHubDto.ReleasesResponse response = githubContract.getReleases(request);

            if (response == null || response.releases() == null) {
                log.warn("GitHub releases 응답이 null: owner={}, repo={}", owner, repo);
                return List.of();
            }

            List<GitHubDto.Release> validReleases = response.releases().stream()
                .filter(r -> !Boolean.TRUE.equals(r.prerelease()))
                .filter(r -> !Boolean.TRUE.equals(r.draft()))
                .toList();

            log.info("GitHub releases 조회 완료: owner={}, repo={}, total={}, valid={}",
                owner, repo, response.releases().size(), validReleases.size());

            return validReleases;
        } catch (Exception e) {
            log.error("GitHub releases 조회 실패: owner={}, repo={}", owner, repo, e);
            return List.of();
        }
    }
}
