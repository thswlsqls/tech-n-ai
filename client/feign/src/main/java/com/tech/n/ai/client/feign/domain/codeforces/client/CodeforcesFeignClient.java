package com.tech.n.ai.client.feign.domain.codeforces.client;

import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "CodeforcesFeign", url = "${feign-clients.codeforces.uri}")
public interface CodeforcesFeignClient {

    @GetMapping(value = "/api/contest.list",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    CodeforcesDto.ContestListResponse getContestList(@RequestParam("gym") Boolean gym);

}
