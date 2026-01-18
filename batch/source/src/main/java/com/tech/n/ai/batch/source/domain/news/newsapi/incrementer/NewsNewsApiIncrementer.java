package com.tech.n.ai.batch.source.domain.news.newsapi.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
public class NewsNewsApiIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";

    private String baseDate;
    private String query;
    private String sources;
    private String domains;
    private String excludeDomains;
    private String from;
    private String to;
    private String language;
    private String sortBy;
    private String pageSize;
    private String page;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        JobParametersBuilder builder = new JobParametersBuilder()
            .addLong(RUN_ID, UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate);
        
        if (query != null) {
            builder.addString("query", query);
        }
        if (sources != null) {
            builder.addString("sources", sources);
        }
        if (domains != null) {
            builder.addString("domains", domains);
        }
        if (excludeDomains != null) {
            builder.addString("excludeDomains", excludeDomains);
        }
        if (from != null) {
            builder.addString("from", from);
        }
        if (to != null) {
            builder.addString("to", to);
        }
        if (language != null) {
            builder.addString("language", language);
        }
        if (sortBy != null) {
            builder.addString("sortBy", sortBy);
        }
        if (pageSize != null) {
            builder.addString("pageSize", pageSize);
        }
        if (page != null) {
            builder.addString("page", page);
        }
        
        return builder.toJobParameters();
    }
}
