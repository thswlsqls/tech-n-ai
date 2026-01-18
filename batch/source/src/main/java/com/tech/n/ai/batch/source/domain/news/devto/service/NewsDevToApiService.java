package com.tech.n.ai.batch.source.domain.news.devto.service;

import com.tech.n.ai.client.feign.domain.devto.contract.DevToContract;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsDevToApiService {

    private final DevToContract devToApi;

    public List<DevToDto.Article> getArticles(String tag, String username, String state, Integer top, Integer collectionId, Integer page, Integer perPage) {
        DevToDto.ArticlesResponse response = devToApi.getArticles(
            DevToDto.ArticlesRequest.builder()
                .tag(tag)
                .username(username)
                .state(state)
                .top(top)
                .collectionId(collectionId)
                .page(page)
                .perPage(perPage)
                .build());
        return response.articles();
    }
}
