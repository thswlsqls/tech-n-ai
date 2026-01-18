package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.GoogleTokenResponse;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.GoogleUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "GoogleOAuth", url = "${feign-clients.oauth.google.uri}")
public interface GoogleOAuthFeignClient {
    
    @PostMapping(
        value = "/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    GoogleTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
    
    @GetMapping(value = "/oauth2/v2/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    GoogleUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
