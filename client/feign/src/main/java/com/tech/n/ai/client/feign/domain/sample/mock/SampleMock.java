package com.tech.n.ai.client.feign.domain.sample.mock;


import com.tech.n.ai.client.feign.domain.sample.contract.SampleContract;
import com.tech.n.ai.client.feign.domain.sample.contract.SampleDto.SampleApiRequest;
import com.tech.n.ai.client.feign.domain.sample.contract.SampleDto.SampleApiResponse;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SampleMock implements SampleContract {

    @Override
    public SampleApiResponse getSample(SampleApiRequest request) {
        log.info("getSample: request={}", request);
        return SampleApiResponse.builder().build();
    }

}