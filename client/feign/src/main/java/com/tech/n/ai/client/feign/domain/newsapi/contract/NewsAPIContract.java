package com.tech.n.ai.client.feign.domain.newsapi.contract;

public interface NewsAPIContract {

    NewsAPIDto.EverythingResponse getEverything(NewsAPIDto.EverythingRequest request);

}
