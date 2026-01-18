package com.tech.n.ai.client.feign.domain.github.api;

import com.tech.n.ai.client.feign.domain.github.client.GitHubFeignClient;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.EventsRequest;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.EventsResponse;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GitHubApi implements GitHubContract {

    private final GitHubFeignClient githubFeign;

    @Value("${feign-clients.github.token:}")
    private String token;

    @Override
    public EventsResponse getEvents(EventsRequest request) {
        String authorization = token != null && !token.isEmpty() ? "Bearer " + token : null;
        List<Event> events = githubFeign.getEvents(
                authorization,
                request.perPage(),
                request.page()
        );
        return EventsResponse.builder()
                .events(events != null ? events : List.of())
                .build();
    }

}
