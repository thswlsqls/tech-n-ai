package com.tech.n.ai.client.feign.domain.codeforces.mock;

import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesContract;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.ContestListRequest;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.ContestListResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class CodeforcesMock implements CodeforcesContract {

    @Override
    public ContestListResponse getContestList(ContestListRequest request) {
        log.info("getContestList: request={}", request);
        return ContestListResponse.builder()
                .status("OK")
                .result(Collections.emptyList())
                .build();
    }

}
