package com.tech.n.ai.batch.source.domain.news.reddit.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
public class NewsRedditIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";

    private String baseDate;
    private String subreddit;
    private String limit;
    private String after;
    private String before;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        JobParametersBuilder builder = new JobParametersBuilder()
            .addLong(RUN_ID, UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate);
        
        if (subreddit != null) {
            builder.addString("subreddit", subreddit);
        }
        if (limit != null) {
            builder.addString("limit", limit);
        }
        if (after != null) {
            builder.addString("after", after);
        }
        if (before != null) {
            builder.addString("before", before);
        }
        
        return builder.toJobParameters();
    }
}
