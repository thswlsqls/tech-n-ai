package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.newsapi.api.NewsAPIApi;
import com.tech.n.ai.client.feign.domain.newsapi.client.NewsAPIFeignClient;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIContract;
import com.tech.n.ai.client.feign.domain.newsapi.mock.NewsAPIMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        NewsAPIFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class NewsAPIFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.newsapi.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public NewsAPIContract newsAPIMock() {
        return new NewsAPIMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public NewsAPIContract newsAPIApi(NewsAPIFeignClient feignClient) {
        return new NewsAPIApi(feignClient);
    }

}
