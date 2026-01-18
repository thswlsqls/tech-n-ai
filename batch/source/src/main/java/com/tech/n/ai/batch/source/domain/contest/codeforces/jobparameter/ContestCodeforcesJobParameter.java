package com.tech.n.ai.batch.source.domain.contest.codeforces.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class ContestCodeforcesJobParameter extends CommonParameter {

    @Value("#{jobParameters[gym]}")
    private String gym;

}

