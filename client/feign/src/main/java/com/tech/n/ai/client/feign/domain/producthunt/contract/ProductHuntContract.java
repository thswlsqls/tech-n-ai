package com.tech.n.ai.client.feign.domain.producthunt.contract;

public interface ProductHuntContract {

    ProductHuntDto.GraphQLResponse executeQuery(ProductHuntDto.GraphQLRequest request);

}
