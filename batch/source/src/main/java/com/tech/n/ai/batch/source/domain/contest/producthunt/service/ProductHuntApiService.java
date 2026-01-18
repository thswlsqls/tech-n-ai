package com.tech.n.ai.batch.source.domain.contest.producthunt.service;

import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntContract;
import com.tech.n.ai.client.feign.domain.producthunt.contract.ProductHuntDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductHuntApiService {

    private final ProductHuntContract productHuntApi;

    /**
     * ProductHunt GraphQL API를 통해 posts 조회
     * GraphQL 쿼리를 사용하여 posts 데이터를 가져옵니다.
     */
    public List<Map<String, Object>> getPosts(Integer first, String after) {
        // GraphQL 쿼리 생성
        String query = """
            query GetPosts($first: Int, $after: String) {
              posts(first: $first, after: $after) {
                edges {
                  node {
                    id
                    name
                    tagline
                    description
                    createdAt
                    featuredAt
                    url
                    website
                    topics {
                      edges {
                        node {
                          name
                        }
                      }
                    }
                  }
                }
                pageInfo {
                  hasNextPage
                  endCursor
                }
              }
            }
            """;

        Map<String, Object> variables = new HashMap<>();
        if (first != null) {
            variables.put("first", first);
        }
        if (after != null) {
            variables.put("after", after);
        }

        ProductHuntDto.GraphQLRequest request = ProductHuntDto.GraphQLRequest.builder()
            .query(query)
            .variables(variables)
            .operationName("GetPosts")
            .build();

        ProductHuntDto.GraphQLResponse response = productHuntApi.executeQuery(request);

        if (response.errors() != null && !response.errors().isEmpty()) {
            log.error("ProductHunt GraphQL errors: {}", response.errors());
            throw new RuntimeException("ProductHunt GraphQL query failed: " + response.errors());
        }

        // GraphQL 응답에서 posts 데이터 추출
        if (response.data() != null) {
            Map<String, Object> postsData = (Map<String, Object>) response.data().get("posts");
            if (postsData != null) {
                List<Map<String, Object>> edges = (List<Map<String, Object>>) postsData.get("edges");
                if (edges != null) {
                    return edges.stream()
                        .map(edge -> (Map<String, Object>) edge.get("node"))
                        .toList();
                }
            }
        }

        return List.of();
    }
}
