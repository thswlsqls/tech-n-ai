package com.tech.n.ai.batch.source.domain.news.devto.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class NewsDevToJobParameter extends CommonParameter {

    @Value("#{jobParameters[tag]}")
    private String tag;

    @Value("#{jobParameters[username]}")
    private String username;

    @Value("#{jobParameters[state]}")
    private String state;

    @Value("#{jobParameters[top]}")
    private String top;

    @Value("#{jobParameters[collection_id]}")
    private String collectionId;

    @Value("#{jobParameters[page]}")
    private String page;

    @Value("#{jobParameters[per_page]}")
    private String perPage;

}
