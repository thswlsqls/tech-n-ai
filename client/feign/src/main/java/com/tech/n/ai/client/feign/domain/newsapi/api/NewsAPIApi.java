package com.tech.n.ai.client.feign.domain.newsapi.api;

import com.tech.n.ai.client.feign.domain.newsapi.client.NewsAPIFeignClient;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIContract;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto.EverythingRequest;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto.EverythingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RequiredArgsConstructor
public class NewsAPIApi implements NewsAPIContract {

    private final NewsAPIFeignClient newsAPIFeign;

    @Value("${feign-clients.newsapi.api-key:}")
    private String apiKey;

    @Override
    public EverythingResponse getEverything(EverythingRequest request) {
        return newsAPIFeign.getEverything(
                apiKey,
                request.query(),
                request.sources(),
                request.domains(),
                request.excludeDomains(),
                request.from(),
                request.to(),
                request.language(),
                request.sortBy(),
                request.pageSize(),
                request.page()
        );
    }

}
