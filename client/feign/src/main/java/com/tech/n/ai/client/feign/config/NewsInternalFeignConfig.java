package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.internal.api.NewsInternalApi;
import com.tech.n.ai.client.feign.domain.internal.client.NewsInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * News 내부 API FeignConfig
 * OpenFeignConfig를 @Import하여 공통 설정 사용
 */
@EnableFeignClients(clients = {
        NewsInternalFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class NewsInternalFeignConfig {

    @Bean
    public NewsInternalContract newsInternalApi(NewsInternalFeignClient feignClient) {
        return new NewsInternalApi(feignClient);
    }
}
