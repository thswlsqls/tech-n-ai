package com.tech.n.ai.client.feign.domain.agent.client;

import com.tech.n.ai.client.feign.domain.agent.contract.AgentContract;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Agent API FeignClient
 * api-agent 모듈의 API 호출용
 */
@FeignClient(
    name = "agent-api",
    url = "${feign.client.config.agent-api.url}"
)
public interface AgentFeignClient extends AgentContract {
}
