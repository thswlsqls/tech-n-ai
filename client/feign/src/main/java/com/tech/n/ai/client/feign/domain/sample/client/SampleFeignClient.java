package com.tech.n.ai.client.feign.domain.sample.client;

import com.tech.n.ai.client.feign.domain.sample.contract.SampleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "SampleFeign", url = "${feign-clients.sample.uri}")
public interface SampleFeignClient {

    @PostMapping(value="/sample"
        , consumes = MediaType.APPLICATION_JSON_VALUE
        , produces = MediaType.APPLICATION_JSON_VALUE)
    SampleDto.SampleApiResponse getSample(@RequestBody SampleDto.SampleApiRequest request);

}
