package com.tech.n.ai.batch.source.domain.contest.producthunt.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class ContestProductHuntJobParameter extends CommonParameter {

    @Value("#{jobParameters[first]}")
    private String first;

    @Value("#{jobParameters[after]}")
    private String after;

}
