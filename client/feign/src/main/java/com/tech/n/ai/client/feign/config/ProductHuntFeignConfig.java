package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.producthunt.api.ProductHuntApi;
import com.tech.n.ai.client.feign.domain.producthunt.client.ProductHuntFeignClient;
import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntContract;
import com.tech.n.ai.client.feign.domain.producthunt.mock.ProductHuntMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        ProductHuntFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class ProductHuntFeignConfig {

    private static final String CLIENT_MODE = "feign-clients.producthunt.mode";

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public ProductHuntContract productHuntMock() {
        return new ProductHuntMock();
    }

    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public ProductHuntContract productHuntApi(ProductHuntFeignClient feignClient) {
        return new ProductHuntApi(feignClient);
    }

}
