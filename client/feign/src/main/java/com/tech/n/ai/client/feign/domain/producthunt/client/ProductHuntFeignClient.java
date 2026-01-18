package com.tech.n.ai.client.feign.domain.producthunt.client;

import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ProductHuntFeign", url = "${feign-clients.producthunt.uri}")
public interface ProductHuntFeignClient {

    @PostMapping(value = "/api/graphql",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ProductHuntDto.GraphQLResponse executeQuery(
            @RequestHeader("Authorization") String token,
            @RequestBody ProductHuntDto.GraphQLRequest request
    );

}
