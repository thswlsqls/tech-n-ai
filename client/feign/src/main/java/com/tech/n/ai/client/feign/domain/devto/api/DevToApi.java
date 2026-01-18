package com.tech.n.ai.client.feign.domain.devto.api;

import com.tech.n.ai.client.feign.domain.devto.client.DevToFeignClient;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToContract;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.ArticlesRequest;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.ArticlesResponse;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DevToApi implements DevToContract {

    private final DevToFeignClient devToFeign;

    @Value("${feign-clients.devto.api-key:}")
    private String apiKey;

    @Override
    public ArticlesResponse getArticles(ArticlesRequest request) {
        List<Article> articles = devToFeign.getArticles(
                apiKey,
                request.tag(),
                request.username(),
                request.state(),
                request.top(),
                request.collectionId(),
                request.page(),
                request.perPage()
        );
        return ArticlesResponse.builder()
                .articles(articles != null ? articles : List.of())
                .build();
    }

}
