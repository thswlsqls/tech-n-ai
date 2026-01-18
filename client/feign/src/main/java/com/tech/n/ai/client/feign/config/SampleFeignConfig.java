package com.tech.n.ai.client.feign.config;


import com.tech.n.ai.client.feign.domain.sample.api.SampleApi;
import com.tech.n.ai.client.feign.domain.sample.client.SampleFeignClient;
import com.tech.n.ai.client.feign.domain.sample.contract.SampleContract;
import com.tech.n.ai.client.feign.domain.sample.mock.SampleMock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@EnableFeignClients(clients = {
    SampleFeignClient.class,
})
@Import({
    OpenFeignConfig.class
})
@Configuration
public class SampleFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.sample.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public SampleContract sampleMock() { return new SampleMock(); }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public SampleContract sampleApi(SampleFeignClient feignClient) { return new SampleApi(feignClient); }

}
