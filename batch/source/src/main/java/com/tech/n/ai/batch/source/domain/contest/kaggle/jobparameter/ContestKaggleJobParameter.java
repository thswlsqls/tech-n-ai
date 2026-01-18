package com.tech.n.ai.batch.source.domain.contest.kaggle.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class ContestKaggleJobParameter extends CommonParameter {

    @Value("#{jobParameters[page]}")
    private String page;

    @Value("#{jobParameters[search]}")
    private String search;

    @Value("#{jobParameters[category]}")
    private String category;

    @Value("#{jobParameters[sortBy]}")
    private String sortBy;

    @Value("#{jobParameters[group]}")
    private String group;

    @Value("#{jobParameters[filter]}")
    private String filter;

}
