package com.tech.n.ai.batch.source.domain.contest.reddit.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class ContestRedditJobParameter extends CommonParameter {

    @Value("#{jobParameters[subreddit]}")
    private String subreddit;

    @Value("#{jobParameters[limit]}")
    private String limit;

    @Value("#{jobParameters[after]}")
    private String after;

    @Value("#{jobParameters[before]}")
    private String before;

}
