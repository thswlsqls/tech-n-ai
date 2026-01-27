package com.tech.n.ai.batch.source.domain.contest.devto.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
public class ContestDevToIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";

    private String baseDate;
    private String tag;
    private String username;
    private String state;
    private String top;
    private String collectionId;
    private String page;
    private String perPage;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        JobParametersBuilder builder = new JobParametersBuilder()
            .addLong(RUN_ID, UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate);
        
        if (tag != null) {
            builder.addString("tag", tag);
        }
        if (username != null) {
            builder.addString("username", username);
        }
        if (state != null) {
            builder.addString("state", state);
        }
        if (top != null) {
            builder.addString("top", top);
        }
        if (collectionId != null) {
            builder.addString("collection_id", collectionId);
        }
        if (page != null) {
            builder.addString("page", page);
        }
        if (perPage != null) {
            builder.addString("per_page", perPage);
        }
        
        return builder.toJobParameters();
    }
}
