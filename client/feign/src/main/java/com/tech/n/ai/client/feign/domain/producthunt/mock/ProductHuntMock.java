package com.tech.n.ai.client.feign.domain.producthunt.mock;

import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntContract;
import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntDto.GraphQLRequest;
import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntDto.GraphQLResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;

@Slf4j
public class ProductHuntMock implements ProductHuntContract {

    @Override
    public GraphQLResponse executeQuery(GraphQLRequest request) {
        log.info("executeQuery: request={}", request);
        return GraphQLResponse.builder()
                .data(new HashMap<>())
                .errors(Collections.emptyList())
                .build();
    }

}
