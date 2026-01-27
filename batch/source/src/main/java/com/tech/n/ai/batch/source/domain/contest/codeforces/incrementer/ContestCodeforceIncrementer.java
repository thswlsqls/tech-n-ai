package com.tech.n.ai.batch.source.domain.contest.codeforces.incrementer;

import com.tech.n.ai.batch.source.common.incrementer.UniqueRunIdIncrementer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

@Slf4j
@RequiredArgsConstructor
public class ContestCodeforceIncrementer extends RunIdIncrementer {

    private static final String RUN_ID_KEY = "run.id";
    private static final String BASE_DATE_KEY = "baseDate";
    private static final String GYM_KEY = "gym";
    private static final long DEFAULT_RUN_ID = 0L;

    private final String baseDate;
    private final String gym;

    @Override
    public JobParameters getNext(JobParameters parameters) {
        JobParameters params = parameters != null ? parameters : new JobParameters();
        long nextRunId = UniqueRunIdIncrementer.safeGetLong(params, RUN_ID_KEY, DEFAULT_RUN_ID) + 1;

        return new JobParametersBuilder()
            .addLong(RUN_ID_KEY, nextRunId)
            .addString(BASE_DATE_KEY, baseDate)
            .addString(GYM_KEY, gym)
            .toJobParameters();
    }
}

