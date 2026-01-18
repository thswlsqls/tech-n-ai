package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.KakaoTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "KakaoOAuth", url = "${feign-clients.oauth.kakao.uri}")
public interface KakaoOAuthFeignClient {
    
    @PostMapping(
        value = "/oauth/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    KakaoTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
}

