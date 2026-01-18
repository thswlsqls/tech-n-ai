package com.tech.n.ai.api.gateway.domain.sample.repository.reader;


import com.tech.n.ai.api.gateway.domain.sample.service.SampleReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SampleReaderImpl implements SampleReader {

    private final SampleQueryDslReaderRepository sampleQueryDslReaderRepository;
    private final SampleTable1JpaReaderRepository sampleTable1JpaReaderRepository;

    /** method implementations ... */

}