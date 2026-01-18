package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.NaverTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "NaverOAuth", url = "${feign-clients.oauth.naver.uri}")
public interface NaverOAuthFeignClient {
    
    @PostMapping(
        value = "/oauth2.0/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    NaverTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
}

