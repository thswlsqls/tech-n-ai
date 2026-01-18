package com.tech.n.ai.client.feign.domain.codeforces.contract;

public interface CodeforcesContract {

    CodeforcesDto.ContestListResponse getContestList(CodeforcesDto.ContestListRequest request);

}
