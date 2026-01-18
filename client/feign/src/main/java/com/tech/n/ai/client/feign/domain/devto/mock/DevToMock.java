package com.tech.n.ai.client.feign.domain.devto.mock;

import com.tech.n.ai.client.feign.domain.devto.contract.DevToContract;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.ArticlesRequest;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.ArticlesResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class DevToMock implements DevToContract {

    @Override
    public ArticlesResponse getArticles(ArticlesRequest request) {
        log.info("getArticles: request={}", request);
        return ArticlesResponse.builder()
                .articles(Collections.emptyList())
                .build();
    }

}
