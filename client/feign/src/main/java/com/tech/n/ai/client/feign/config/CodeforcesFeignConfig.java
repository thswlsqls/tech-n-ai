package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.codeforces.api.CodeforcesApi;
import com.tech.n.ai.client.feign.domain.codeforces.client.CodeforcesFeignClient;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesContract;
import com.tech.n.ai.client.feign.domain.codeforces.mock.CodeforcesMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        CodeforcesFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class CodeforcesFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.codeforces.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public CodeforcesContract codeforcesMock() {
        return new CodeforcesMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public CodeforcesContract codeforcesApi(CodeforcesFeignClient feignClient) {
        return new CodeforcesApi(feignClient);
    }

}
