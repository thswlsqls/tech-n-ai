package com.tech.n.ai.client.feign.domain.codeforces.api;


import com.tech.n.ai.client.feign.domain.codeforces.client.CodeforcesFeignClient;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesContract;

import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.ContestListRequest;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.ContestListResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CodeforcesApi implements CodeforcesContract {

    private final CodeforcesFeignClient codeforcesFeign;

    @Override
    public ContestListResponse getContestList(ContestListRequest request) {
        return codeforcesFeign.getContestList(request.gym());
    }

}
