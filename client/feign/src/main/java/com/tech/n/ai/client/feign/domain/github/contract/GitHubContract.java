package com.tech.n.ai.client.feign.domain.github.contract;

public interface GitHubContract {

    GitHubDto.EventsResponse getEvents(GitHubDto.EventsRequest request);

    /**
     * GitHub Releases 조회
     */
    GitHubDto.ReleasesResponse getReleases(GitHubDto.ReleasesRequest request);

}
