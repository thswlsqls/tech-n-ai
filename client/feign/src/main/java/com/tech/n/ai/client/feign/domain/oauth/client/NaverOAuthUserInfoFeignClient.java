package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.NaverUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "NaverOAuthUserInfo", url = "${feign-clients.oauth.naver.userinfo.uri}")
public interface NaverOAuthUserInfoFeignClient {
    
    @GetMapping(value = "/v1/nid/me", produces = MediaType.APPLICATION_JSON_VALUE)
    NaverUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
