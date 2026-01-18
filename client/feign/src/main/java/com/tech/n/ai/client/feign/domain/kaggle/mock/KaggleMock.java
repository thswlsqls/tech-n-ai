package com.tech.n.ai.client.feign.domain.kaggle.mock;

import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleContract;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto.CompetitionsListRequest;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto.CompetitionsListResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class KaggleMock implements KaggleContract {

    @Override
    public CompetitionsListResponse getCompetitionsList(CompetitionsListRequest request) {
        log.info("getCompetitionsList: request={}", request);
        return CompetitionsListResponse.builder()
                .ref("mock")
                .totalResults(0)
                .page(1)
                .pageSize(20)
                .results(Collections.emptyList())
                .build();
    }

}
