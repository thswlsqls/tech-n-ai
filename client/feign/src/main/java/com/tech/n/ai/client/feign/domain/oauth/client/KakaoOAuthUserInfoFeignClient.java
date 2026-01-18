package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.KakaoUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "KakaoOAuthUserInfo", url = "${feign-clients.oauth.kakao.userinfo.uri}")
public interface KakaoOAuthUserInfoFeignClient {
    
    @GetMapping(value = "/v2/user/me", produces = MediaType.APPLICATION_JSON_VALUE)
    KakaoUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
