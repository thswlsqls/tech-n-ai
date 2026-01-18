package com.tech.n.ai.client.feign.domain.newsapi.client;

import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "NewsAPIFeign", url = "${feign-clients.newsapi.uri}")
public interface NewsAPIFeignClient {

    @GetMapping(value = "/everything",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    NewsAPIDto.EverythingResponse getEverything(
            @RequestParam("apiKey") String apiKey,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "sources", required = false) String sources,
            @RequestParam(value = "domains", required = false) String domains,
            @RequestParam(value = "excludeDomains", required = false) String excludeDomains,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "page", required = false) Integer page
    );

}
