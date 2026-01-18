package com.tech.n.ai.batch.source.domain.news.newsapi.service;

import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIContract;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsApiService {

    private final NewsAPIContract newsApi;

    public List<NewsAPIDto.Article> getEverything(String query, String sources, String domains, 
                                                   String excludeDomains, String from, String to,
                                                   String language, String sortBy, Integer pageSize, Integer page) {
        NewsAPIDto.EverythingResponse response = newsApi.getEverything(
            NewsAPIDto.EverythingRequest.builder()
                .query(query)
                .sources(sources)
                .domains(domains)
                .excludeDomains(excludeDomains)
                .from(from)
                .to(to)
                .language(language)
                .sortBy(sortBy)
                .pageSize(pageSize)
                .page(page)
                .build());
        return response.articles() != null ? response.articles() : List.of();
    }
}
