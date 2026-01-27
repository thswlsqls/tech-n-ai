package com.tech.n.ai.client.feign.domain.internal.client;

import com.tech.n.ai.client.feign.domain.internal.contract.AiUpdateInternalContract;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * AI Update 내부 API FeignClient
 * api-ai-update 모듈의 내부 API 호출용
 */
@FeignClient(
    name = "ai-update-internal-api",
    url = "${feign.client.config.ai-update-internal-api.url}"
)
public interface AiUpdateInternalFeignClient extends AiUpdateInternalContract {
}
