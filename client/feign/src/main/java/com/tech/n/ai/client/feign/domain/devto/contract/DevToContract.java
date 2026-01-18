package com.tech.n.ai.client.feign.domain.devto.contract;

public interface DevToContract {

    DevToDto.ArticlesResponse getArticles(DevToDto.ArticlesRequest request);

}
