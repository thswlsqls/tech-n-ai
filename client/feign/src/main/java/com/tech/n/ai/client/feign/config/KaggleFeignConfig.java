package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.kaggle.api.KaggleApi;
import com.tech.n.ai.client.feign.domain.kaggle.client.KaggleFeignClient;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleContract;
import com.tech.n.ai.client.feign.domain.kaggle.mock.KaggleMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        KaggleFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class KaggleFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.kaggle.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public KaggleContract kaggleMock() {
        return new KaggleMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public KaggleContract kaggleApi(KaggleFeignClient feignClient) {
        return new KaggleApi(feignClient);
    }

}
