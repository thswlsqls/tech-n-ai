package com.tech.n.ai.api.gateway.domain.sample.facade;


import com.tech.n.ai.api.gateway.domain.sample.service.SampleCategory1Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SampleFacade {

    private final SampleCategory1Service sampleCategory1Service;
    
}
