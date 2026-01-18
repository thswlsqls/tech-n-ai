package com.tech.n.ai.client.feign.domain.devto.client;

import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "DevToFeign", url = "${feign-clients.devto.uri}")
public interface DevToFeignClient {

    @GetMapping(value = "/articles",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    List<DevToDto.Article> getArticles(
            @RequestHeader(value = "api-key", required = false) String apiKey,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "top", required = false) Integer top,
            @RequestParam(value = "collection_id", required = false) Integer collectionId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "per_page", required = false) Integer perPage
    );

}
