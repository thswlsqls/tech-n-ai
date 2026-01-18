package com.tech.n.ai.client.feign.domain.producthunt.api;

import com.tech.n.ai.client.feign.domain.producthunt.client.ProductHuntFeignClient;
import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntContract;
import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntDto.GraphQLRequest;
import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntDto.GraphQLResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RequiredArgsConstructor
public class ProductHuntApi implements ProductHuntContract {

    private final ProductHuntFeignClient productHuntFeign;

    @Value("${feign-clients.producthunt.token:}")
    private String token;

    @Override
    public GraphQLResponse executeQuery(GraphQLRequest request) {
        String authorization = "Bearer " + token;
        return productHuntFeign.executeQuery(authorization, request);
    }

}
