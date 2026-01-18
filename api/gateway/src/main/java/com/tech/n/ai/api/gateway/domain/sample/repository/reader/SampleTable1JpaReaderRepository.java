package com.tech.n.ai.api.gateway.domain.sample.repository.reader;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tech.n.ai.datasource.aurora.entity.SampleTable1Entity;

public interface SampleTable1JpaReaderRepository extends JpaRepository<SampleTable1Entity, Long>, SampleTable1ReaderCustomJpaRepository{
    
}
