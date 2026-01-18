package com.tech.n.ai.client.feign.domain.sample.api;


import com.tech.n.ai.client.feign.domain.sample.client.SampleFeignClient;
import com.tech.n.ai.client.feign.domain.sample.contract.SampleContract;
import com.tech.n.ai.client.feign.domain.sample.contract.SampleDto.SampleApiRequest;
import com.tech.n.ai.client.feign.domain.sample.contract.SampleDto.SampleApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
public class SampleApi implements SampleContract {

    private final SampleFeignClient sampleFeign;

    @Override
    public SampleApiResponse getSample(SampleApiRequest request) {
        return sampleFeign.getSample(request);
    }

}
