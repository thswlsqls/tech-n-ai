package com.tech.n.ai.batch.source.domain.contest.hackernews.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.hackernews.incrementer.ContestHackerNewsIncrementer;
import com.tech.n.ai.batch.source.domain.contest.hackernews.jobparameter.ContestHackerNewsJobParameter;
import com.tech.n.ai.batch.source.domain.contest.hackernews.processor.HackerNewsStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.hackernews.reader.HackerNewsApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.hackernews.service.HackerNewsApiService;
import com.tech.n.ai.batch.source.domain.contest.hackernews.writer.HackerNewsStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemResponse;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestHackerNewsApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${limit:#{null}}")
    private String limit;

    private final HackerNewsApiService service;
    private final ContestHackerNewsJobParameter parameter;
    private final ContestInternalContract contestInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name=Constants.CONTEST_HACKERNEWS + Constants.PARAMETER)
    @JobScope
    public ContestHackerNewsJobParameter parameter() { 
        return new ContestHackerNewsJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_HACKERNEWS)
    public Job ContestHackerNewsJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_HACKERNEWS + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_HACKERNEWS, jobRepository)
            .start(step1)
            .incrementer(new ContestHackerNewsIncrementer(baseDate, limit))
            .build();
    }

    @Bean(name = Constants.CONTEST_HACKERNEWS + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(Constants.CONTEST_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_READER) HackerNewsApiPagingItemReader<ItemResponse> reader,
                      @Qualifier(Constants.CONTEST_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_PROCESSOR) HackerNewsStep1Processor processor,
                      @Qualifier(Constants.CONTEST_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_WRITER) HackerNewsStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_HACKERNEWS + Constants.STEP_1, jobRepository)
            .<ItemResponse, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public HackerNewsApiPagingItemReader<ItemResponse> step1Reader(
            @Value("#{jobParameters['limit']}") String limit) {
        Integer limitValue = (limit != null && !limit.isBlank()) 
            ? Integer.parseInt(limit) 
            : null;
        
        return new HackerNewsApiPagingItemReader<ItemResponse>(
            Constants.CHUNK_SIZE_10, 
            service,
            limitValue);
    }

    @Bean(name = Constants.CONTEST_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public HackerNewsStep1Processor step1Processor() {
        return new HackerNewsStep1Processor(redisTemplate);
    }

    @Bean(name = Constants.CONTEST_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public HackerNewsStep1Writer step1Writer() {
        return new HackerNewsStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_HACKERNEWS + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
