package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.internal.api.EmergingTechInternalApi;
import com.tech.n.ai.client.feign.domain.internal.client.EmergingTechInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Emerging Tech 내부 API FeignConfig
 */
@EnableFeignClients(clients = {
        EmergingTechInternalFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class EmergingTechInternalFeignConfig {

    @Bean
    public EmergingTechInternalContract emergingTechInternalApi(EmergingTechInternalFeignClient feignClient) {
        return new EmergingTechInternalApi(feignClient);
    }
}
