package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.reddit.api.RedditApi;
import com.tech.n.ai.client.feign.domain.reddit.client.RedditFeignClient;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditContract;
import com.tech.n.ai.client.feign.domain.reddit.mock.RedditMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        RedditFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class RedditFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.reddit.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public RedditContract redditMock() {
        return new RedditMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public RedditContract redditApi(RedditFeignClient feignClient) {
        return new RedditApi(feignClient);
    }

}
