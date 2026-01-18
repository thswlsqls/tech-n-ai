package com.tech.n.ai.client.feign.domain.internal.client;

import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Contest 내부 API FeignClient
 * api-contest 모듈의 내부 API를 호출하기 위한 Feign Client
 */
@FeignClient(
    name = "contest-internal-api",
    url = "${feign.client.config.contest-internal-api.url}"
)
public interface ContestInternalFeignClient extends ContestInternalContract {
}
