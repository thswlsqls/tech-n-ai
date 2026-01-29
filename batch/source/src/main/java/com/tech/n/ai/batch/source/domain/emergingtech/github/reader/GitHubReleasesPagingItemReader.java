package com.tech.n.ai.batch.source.domain.emergingtech.github.reader;

import com.tech.n.ai.batch.source.domain.emergingtech.github.service.GitHubReleasesService;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GitHub Releases 페이징 Item Reader
 * 여러 저장소의 Releases를 순차적으로 조회하고 저장소 정보와 함께 반환
 */
@Slf4j
public class GitHubReleasesPagingItemReader extends AbstractPagingItemReader<GitHubReleaseWithRepo> {

    private final GitHubReleasesService service;
    private final List<RepositoryInfo> repositories;
    private List<GitHubReleaseWithRepo> cachedReleases;

    public GitHubReleasesPagingItemReader(int pageSize, GitHubReleasesService service, List<RepositoryInfo> repositories) {
        setPageSize(pageSize);
        this.service = service;
        this.repositories = repositories;
    }

    @Override
    protected void doReadPage() {
        initResults();
        fetchAndCacheIfNeeded();
        addPageItemsToResults();
    }

    @Override
    protected void doOpen() {
        log.info("Opening GitHub Releases reader: repositories={}", repositories.size());
    }

    @Override
    protected void doClose() {
        log.info("Closing GitHub Releases reader");
        cachedReleases = null;
    }

    private void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    /**
     * 모든 저장소의 Releases를 한 번에 가져와 캐싱 (저장소 정보 포함)
     */
    private void fetchAndCacheIfNeeded() {
        if (cachedReleases == null) {
            cachedReleases = new ArrayList<>();

            for (RepositoryInfo repo : repositories) {
                try {
                    List<GitHubDto.Release> releases = service.getReleases(
                        repo.owner(),
                        repo.repo(),
                        10,  // 각 저장소당 최신 10개
                        1
                    );

                    // Release에 저장소 정보를 함께 저장
                    for (GitHubDto.Release release : releases) {
                        cachedReleases.add(GitHubReleaseWithRepo.builder()
                            .release(release)
                            .owner(repo.owner())
                            .repo(repo.repo())
                            .provider(repo.provider())
                            .build());
                    }

                    log.info("Fetched {} releases from {}/{}", releases.size(), repo.owner(), repo.repo());
                } catch (Exception e) {
                    log.error("Failed to fetch releases from {}/{}: {}", repo.owner(), repo.repo(), e.getMessage());
                }
            }

            log.info("Total cached releases: {}", cachedReleases.size());
        }
    }

    private void addPageItemsToResults() {
        int page = getPage();
        int pageSize = getPageSize();
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, cachedReleases.size());

        if (fromIndex >= cachedReleases.size()) {
            return;
        }

        List<GitHubReleaseWithRepo> pageItems = cachedReleases.subList(fromIndex, toIndex);
        results.addAll(pageItems);

        log.debug("Page {}: reading items {} to {} (count: {})", page, fromIndex, toIndex - 1, pageItems.size());
    }

    /**
     * 저장소 정보
     */
    public record RepositoryInfo(String owner, String repo, String provider) {}
}
