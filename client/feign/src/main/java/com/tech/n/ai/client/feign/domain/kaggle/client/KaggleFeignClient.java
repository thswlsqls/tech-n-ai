package com.tech.n.ai.client.feign.domain.kaggle.client;

import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "KaggleFeign", url = "${feign-clients.kaggle.uri}")
public interface KaggleFeignClient {

    @GetMapping(value = "/competitions/list",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    KaggleDto.CompetitionsListResponse getCompetitionsList(
            @RequestHeader("Authorization") String apiKey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "filter", required = false) String filter
    );

}
