package com.tech.n.ai.client.feign.domain.internal.client;

import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Emerging Tech 내부 API FeignClient
 * api-emerging-tech 모듈의 내부 API 호출용
 */
@FeignClient(
    name = "emerging-tech-internal-api",
    url = "${feign.client.config.emerging-tech-internal-api.url}"
)
public interface EmergingTechInternalFeignClient extends EmergingTechInternalContract {
}
