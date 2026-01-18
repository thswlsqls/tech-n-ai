package com.tech.n.ai.batch.source.domain.contest.codeforces.jobconfig;


import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.codeforces.incrementer.ContestCodeforceIncrementer;
import com.tech.n.ai.batch.source.domain.contest.codeforces.jobparameter.ContestCodeforcesJobParameter;
import com.tech.n.ai.batch.source.domain.contest.codeforces.processor.CodeforcesStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.codeforces.reader.CodeforcesApiPagingItemReader;

import com.tech.n.ai.batch.source.domain.contest.codeforces.service.CodeforcesApiService;
import com.tech.n.ai.batch.source.domain.contest.codeforces.writer.CodeforcesStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesContract;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.Contest;

import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestCodeforcesJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${gym:#{false}}")
    private String gym;

    private final CodeforcesApiService service;
    private final ContestCodeforcesJobParameter parameter;
    private final ContestInternalContract contestInternalApi;


    @Bean(name=Constants.CONTEST_CODEFORCES + Constants.PARAMETER)
    @JobScope
    public ContestCodeforcesJobParameter parameter() { return new ContestCodeforcesJobParameter(); }

    @Bean(name=Constants.CONTEST_CODEFORCES)
    public Job ContestCodeforcesJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_CODEFORCES + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_CODEFORCES, jobRepository)
            .start(step1)
            .incrementer(new ContestCodeforceIncrementer(baseDate, gym))
            .build();
    }

    @Bean(name = Constants.CONTEST_CODEFORCES + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_CODEFORCES + Constants.STEP_1 + Constants.ITEM_READER) CodeforcesApiPagingItemReader<Contest> reader,
                      @Qualifier(Constants.CONTEST_CODEFORCES + Constants.STEP_1 + Constants.ITEM_PROCESSOR) CodeforcesStep1Processor processor,
                      @Qualifier(Constants.CONTEST_CODEFORCES + Constants.STEP_1 + Constants.ITEM_WRITER) CodeforcesStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_CODEFORCES + Constants.STEP_1, jobRepository)
            .<Contest, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_CODEFORCES + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public CodeforcesApiPagingItemReader<Contest> step1Reader(
            @Value("#{jobParameters['gym']}") String gym) {
        return new CodeforcesApiPagingItemReader<Contest>(Constants.CHUNK_SIZE_10
            , service
            , Boolean.parseBoolean(gym != null ? gym : "false"));
    }

    @Bean(name = Constants.CONTEST_CODEFORCES + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public CodeforcesStep1Processor step1Processor() {
        return new CodeforcesStep1Processor();
    }

    @Bean(name = Constants.CONTEST_CODEFORCES + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public CodeforcesStep1Writer step1Writer() {
        return new CodeforcesStep1Writer(contestInternalApi);
    }


    @Bean(name = Constants.CONTEST_CODEFORCES + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }

}
