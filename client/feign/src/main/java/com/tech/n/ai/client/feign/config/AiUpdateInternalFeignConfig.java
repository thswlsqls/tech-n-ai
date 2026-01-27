package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.internal.api.AiUpdateInternalApi;
import com.tech.n.ai.client.feign.domain.internal.client.AiUpdateInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.AiUpdateInternalContract;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * AI Update 내부 API FeignConfig
 */
@EnableFeignClients(clients = {
        AiUpdateInternalFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class AiUpdateInternalFeignConfig {

    @Bean
    public AiUpdateInternalContract aiUpdateInternalApi(AiUpdateInternalFeignClient feignClient) {
        return new AiUpdateInternalApi(feignClient);
    }
}
