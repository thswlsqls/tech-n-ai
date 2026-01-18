package com.tech.n.ai.client.feign.domain.newsapi.mock;

import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIContract;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto.EverythingRequest;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto.EverythingResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class NewsAPIMock implements NewsAPIContract {

    @Override
    public EverythingResponse getEverything(EverythingRequest request) {
        log.info("getEverything: request={}", request);
        return EverythingResponse.builder()
                .status("ok")
                .totalResults(0)
                .articles(Collections.emptyList())
                .build();
    }

}
