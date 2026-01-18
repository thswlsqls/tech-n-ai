package com.tech.n.ai.client.feign.domain.kaggle.api;

import com.tech.n.ai.client.feign.domain.kaggle.client.KaggleFeignClient;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleContract;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto.CompetitionsListRequest;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto.CompetitionsListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RequiredArgsConstructor
public class KaggleApi implements KaggleContract {

    private final KaggleFeignClient kaggleFeign;

    @Value("${feign-clients.kaggle.api-key:}")
    private String apiKey;

    @Override
    public CompetitionsListResponse getCompetitionsList(CompetitionsListRequest request) {
        String authorization = "Bearer " + apiKey;
        return kaggleFeign.getCompetitionsList(
                authorization,
                request.page(),
                request.search(),
                request.category(),
                request.sortBy(),
                request.group(),
                request.filter()
        );
    }

}
