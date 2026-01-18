package com.tech.n.ai.batch.source.domain.news.newsapi.jobparameter;

import com.tech.n.ai.batch.source.common.jobparameter.CommonParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class NewsNewsApiJobParameter extends CommonParameter {

    @Value("#{jobParameters[query]}")
    private String query;

    @Value("#{jobParameters[sources]}")
    private String sources;

    @Value("#{jobParameters[domains]}")
    private String domains;

    @Value("#{jobParameters[excludeDomains]}")
    private String excludeDomains;

    @Value("#{jobParameters[from]}")
    private String from;

    @Value("#{jobParameters[to]}")
    private String to;

    @Value("#{jobParameters[language]}")
    private String language;

    @Value("#{jobParameters[sortBy]}")
    private String sortBy;

    @Value("#{jobParameters[pageSize]}")
    private String pageSize;

    @Value("#{jobParameters[page]}")
    private String page;

}
