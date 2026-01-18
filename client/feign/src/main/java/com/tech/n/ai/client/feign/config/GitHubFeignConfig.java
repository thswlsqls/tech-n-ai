package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.github.api.GitHubApi;
import com.tech.n.ai.client.feign.domain.github.client.GitHubFeignClient;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.mock.GitHubMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        GitHubFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class GitHubFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.github.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public GitHubContract githubMock() {
        return new GitHubMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public GitHubContract githubApi(GitHubFeignClient feignClient) {
        return new GitHubApi(feignClient);
    }

}
