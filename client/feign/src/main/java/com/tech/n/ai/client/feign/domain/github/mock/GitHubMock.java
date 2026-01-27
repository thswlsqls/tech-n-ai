package com.tech.n.ai.client.feign.domain.github.mock;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.EventsRequest;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.EventsResponse;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.ReleasesRequest;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.ReleasesResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class GitHubMock implements GitHubContract {

    @Override
    public EventsResponse getEvents(EventsRequest request) {
        log.info("getEvents: request={}", request);
        return EventsResponse.builder()
                .events(Collections.emptyList())
                .build();
    }

    @Override
    public ReleasesResponse getReleases(ReleasesRequest request) {
        log.info("getReleases: request={}", request);
        return ReleasesResponse.builder()
                .releases(Collections.emptyList())
                .build();
    }

}
