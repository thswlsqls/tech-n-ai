package com.tech.n.ai.batch.source.domain.contest.github.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class ContestGitHubJobParameter extends CommonParameter {

    @Value("#{jobParameters[perPage]}")
    private String perPage;

    @Value("#{jobParameters[page]}")
    private String page;

}
