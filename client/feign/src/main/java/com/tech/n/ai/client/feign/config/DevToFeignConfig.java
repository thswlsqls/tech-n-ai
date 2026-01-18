package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.devto.api.DevToApi;
import com.tech.n.ai.client.feign.domain.devto.client.DevToFeignClient;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToContract;
import com.tech.n.ai.client.feign.domain.devto.mock.DevToMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        DevToFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class DevToFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.devto.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public DevToContract devToMock() {
        return new DevToMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public DevToContract devToApi(DevToFeignClient feignClient) {
        return new DevToApi(feignClient);
    }

}
