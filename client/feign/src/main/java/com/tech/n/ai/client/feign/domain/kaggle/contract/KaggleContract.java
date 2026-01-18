package com.tech.n.ai.client.feign.domain.kaggle.contract;

public interface KaggleContract {

    KaggleDto.CompetitionsListResponse getCompetitionsList(KaggleDto.CompetitionsListRequest request);

}
