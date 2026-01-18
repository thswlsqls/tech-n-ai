package com.tech.n.ai.client.feign.domain.internal.client;

import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * News 내부 API FeignClient
 * api-news 모듈의 내부 API를 호출하기 위한 Feign Client
 */
@FeignClient(
    name = "news-internal-api",
    url = "${feign.client.config.news-internal-api.url}"
)
public interface NewsInternalFeignClient extends NewsInternalContract {
}
