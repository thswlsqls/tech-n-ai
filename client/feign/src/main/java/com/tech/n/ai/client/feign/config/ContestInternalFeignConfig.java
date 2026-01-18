package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.internal.api.ContestInternalApi;
import com.tech.n.ai.client.feign.domain.internal.client.ContestInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Contest 내부 API FeignConfig
 * OpenFeignConfig를 @Import하여 공통 설정 사용
 */
@EnableFeignClients(clients = {
        ContestInternalFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class ContestInternalFeignConfig {

    @Bean
    public ContestInternalContract contestInternalApi(ContestInternalFeignClient feignClient) {
        return new ContestInternalApi(feignClient);
    }
}
