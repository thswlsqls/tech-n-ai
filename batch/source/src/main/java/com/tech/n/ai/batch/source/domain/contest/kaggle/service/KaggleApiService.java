package com.tech.n.ai.batch.source.domain.contest.kaggle.service;

import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleContract;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KaggleApiService {

    private final KaggleContract kaggleApi;

    public List<KaggleDto.Competition> getCompetitionsList(Integer page, String search, String category, String sortBy, String group, String filter) {
        KaggleDto.CompetitionsListResponse response = kaggleApi.getCompetitionsList(
            KaggleDto.CompetitionsListRequest.builder()
                .page(page)
                .search(search)
                .category(category)
                .sortBy(sortBy)
                .group(group)
                .filter(filter)
                .build());
        return response.results();
    }
}
