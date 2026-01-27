package com.tech.n.ai.batch.source.domain.contest.kaggle.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
public class ContestKaggleIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";

    private String baseDate;
    private String page;
    private String search;
    private String category;
    private String sortBy;
    private String group;
    private String filter;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        JobParametersBuilder builder = new JobParametersBuilder()
            .addLong(RUN_ID, UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate);
        
        if (page != null) {
            builder.addString("page", page);
        }
        if (search != null) {
            builder.addString("search", search);
        }
        if (category != null) {
            builder.addString("category", category);
        }
        if (sortBy != null) {
            builder.addString("sortBy", sortBy);
        }
        if (group != null) {
            builder.addString("group", group);
        }
        if (filter != null) {
            builder.addString("filter", filter);
        }
        
        return builder.toJobParameters();
    }
}
