package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.agent.api.AgentApi;
import com.tech.n.ai.client.feign.domain.agent.client.AgentFeignClient;
import com.tech.n.ai.client.feign.domain.agent.contract.AgentContract;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Agent API FeignConfig
 */
@EnableFeignClients(clients = {
        AgentFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class AgentFeignConfig {

    @Bean
    public AgentContract agentApi(AgentFeignClient feignClient) {
        return new AgentApi(feignClient);
    }
}
