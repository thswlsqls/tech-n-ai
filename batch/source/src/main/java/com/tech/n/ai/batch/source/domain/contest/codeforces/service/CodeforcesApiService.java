package com.tech.n.ai.batch.source.domain.contest.codeforces.service;

import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesContract;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.Contest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeforcesApiService {

    private final CodeforcesContract codeforcesApi;

    public List<Contest> getContestList(boolean includeGym) {
        CodeforcesDto.ContestListRequest request = CodeforcesDto.ContestListRequest.builder()
            .gym(includeGym)
            .build();
        
        return codeforcesApi.getContestList(request).result();
    }
}


