package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.hackernews.api.HackerNewsApi;
import com.tech.n.ai.client.feign.domain.hackernews.client.HackerNewsFeignClient;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsContract;
import com.tech.n.ai.client.feign.domain.hackernews.mock.HackerNewsMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        HackerNewsFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class HackerNewsFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.hackernews.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public HackerNewsContract hackerNewsMock() {
        return new HackerNewsMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public HackerNewsContract hackerNewsApi(HackerNewsFeignClient feignClient) {
        return new HackerNewsApi(feignClient);
    }

}
