package com.tech.n.ai.batch.source.domain.contest.github.service;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubApiService {

    private final GitHubContract githubApi;

    public List<GitHubDto.Event> getEvents(Integer perPage, Integer page) {
        GitHubDto.EventsResponse response = githubApi.getEvents(
            GitHubDto.EventsRequest.builder()
                .perPage(perPage)
                .page(page)
                .build());
        return response.events();
    }
}
