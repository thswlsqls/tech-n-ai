package com.tech.n.ai.client.feign.domain.github.client;

import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "GitHubFeign", url = "${feign-clients.github.uri}")
public interface GitHubFeignClient {

    @GetMapping(value = "/events",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<GitHubDto.Event> getEvents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "per_page", required = false) Integer perPage,
            @RequestParam(value = "page", required = false) Integer page
    );

}
