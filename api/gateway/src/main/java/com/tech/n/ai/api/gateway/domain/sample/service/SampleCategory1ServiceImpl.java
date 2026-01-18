package com.tech.n.ai.api.gateway.domain.sample.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleCategory1ServiceImpl implements SampleCategory1Service {

    private final SampleReader sampleReader;
    private final SampleWriter sampleWriter;

    /** method implementations ... */

}
