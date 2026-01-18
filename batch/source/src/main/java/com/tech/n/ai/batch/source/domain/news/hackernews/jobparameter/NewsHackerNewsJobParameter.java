package com.tech.n.ai.batch.source.domain.news.hackernews.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class NewsHackerNewsJobParameter extends CommonParameter {

    @Value("#{jobParameters[limit]}")
    private String limit;

}
