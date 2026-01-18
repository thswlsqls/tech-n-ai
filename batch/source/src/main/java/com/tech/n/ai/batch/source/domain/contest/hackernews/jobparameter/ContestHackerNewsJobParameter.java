package com.tech.n.ai.batch.source.domain.contest.hackernews.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class ContestHackerNewsJobParameter extends CommonParameter {

    @Value("#{jobParameters[limit]}")
    private String limit;

}
