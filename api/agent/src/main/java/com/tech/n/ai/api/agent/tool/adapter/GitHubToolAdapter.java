package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.GitHubReleaseDto;
import com.tech.n.ai.api.agent.tool.util.TextTruncator;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * GitHub API를 LangChain4j Tool 형식으로 래핑하는 어댑터
 * GitHubContract를 통해 GitHub API 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubToolAdapter {

    private static final int RELEASE_BODY_MAX_LENGTH = 500;

    private final GitHubContract githubContract;

    /**
     * GitHub 저장소의 릴리즈 목록 조회
     *
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     * @return 릴리즈 목록 (Draft, Prerelease 제외)
     */
    public List<GitHubReleaseDto> getReleases(String owner, String repo) {
        try {
            GitHubDto.ReleasesRequest request = GitHubDto.ReleasesRequest.builder()
                .owner(owner)
                .repo(repo)
                .perPage(10)
                .page(1)
                .build();

            GitHubDto.ReleasesResponse response = githubContract.getReleases(request);

            if (response == null || response.releases() == null) {
                return List.of();
            }

            return response.releases().stream()
                .filter(r -> !Boolean.TRUE.equals(r.prerelease()) && !Boolean.TRUE.equals(r.draft()))
                .map(r -> new GitHubReleaseDto(
                    r.tagName(),
                    r.name(),
                    TextTruncator.truncate(r.body(), RELEASE_BODY_MAX_LENGTH),
                    r.htmlUrl(),
                    r.publishedAt()
                ))
                .toList();
        } catch (FeignException.Forbidden e) {
            log.error("GitHub API 접근 거부 (403): owner={}, repo={}", owner, repo, e);
            throw new RuntimeException(
                "GitHub API rate limit exceeded. 인증되지 않은 요청은 시간당 60회로 제한됩니다. "
                + "GITHUB_TOKEN 환경변수를 설정하세요. 이 저장소에 대해 더 이상 재시도하지 마세요.");
        } catch (FeignException.TooManyRequests e) {
            log.error("GitHub API rate limit 초과 (429): owner={}, repo={}", owner, repo, e);
            throw new RuntimeException(
                "GitHub API rate limit exceeded (429). 잠시 후 다시 시도하세요. "
                + "이 저장소에 대해 더 이상 재시도하지 마세요.");
        } catch (Exception e) {
            log.error("GitHub releases 조회 실패: owner={}, repo={}", owner, repo, e);
            return List.of();
        }
    }
}
