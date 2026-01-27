package com.tech.n.ai.batch.source.domain.contest.producthunt.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
public class ContestProductHuntIncrementer extends RunIdIncrementer {

    private static final String RUN_ID = "run.id";

    private String baseDate;
    private String first;
    private String after;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        JobParametersBuilder builder = new JobParametersBuilder()
            .addLong(RUN_ID, UniqueRunIdIncrementer.safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate);
        
        if (first != null) {
            builder.addString("first", first);
        }
        if (after != null) {
            builder.addString("after", after);
        }
        
        return builder.toJobParameters();
    }
}
