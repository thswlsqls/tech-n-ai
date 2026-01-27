package com.tech.n.ai.batch.source.domain.contest.atcoder.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.atcoder.incrementer.ContestAtCoderIncrementer;
import com.tech.n.ai.batch.source.domain.contest.atcoder.jobparameter.ContestAtCoderJobParameter;
import com.tech.n.ai.batch.source.domain.contest.atcoder.listener.AtCoderJobListener;
import com.tech.n.ai.batch.source.domain.contest.atcoder.processor.AtCoderStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.atcoder.reader.AtCoderScrapingItemReader;
import com.tech.n.ai.batch.source.domain.contest.atcoder.service.ContestAtCoderScraperService;
import com.tech.n.ai.batch.source.domain.contest.atcoder.writer.AtCoderStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
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
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestAtCoderScraperJobConfig {

    private static final String JOB_NAME = Constants.CONTEST_ATCODER;
    private static final String STEP1_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final ContestAtCoderScraperService service;
    private final ContestAtCoderJobParameter parameter;
    private final ContestInternalContract contestInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name = JOB_NAME + Constants.PARAMETER)
    @JobScope
    public ContestAtCoderJobParameter parameter() {
        return new ContestAtCoderJobParameter();
    }

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository,
                   @Qualifier(STEP1_NAME) Step step1,
                   @Qualifier(JOB_NAME + ".listener") AtCoderJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step1)
            .incrementer(new ContestAtCoderIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = JOB_NAME + ".listener")
    public AtCoderJobListener jobListener() {
        return new AtCoderJobListener(redisTemplate);
    }

    @Bean(name = STEP1_NAME)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP1_NAME + Constants.ITEM_READER) AtCoderScrapingItemReader<ScrapedContestItem> reader,
                      @Qualifier(STEP1_NAME + Constants.ITEM_PROCESSOR) AtCoderStep1Processor processor,
                      @Qualifier(STEP1_NAME + Constants.ITEM_WRITER) AtCoderStep1Writer writer) {
        return new StepBuilder(STEP1_NAME, jobRepository)
            .<ScrapedContestItem, ContestCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_READER)
    @StepScope
    public AtCoderScrapingItemReader<ScrapedContestItem> step1Reader() {
        return new AtCoderScrapingItemReader<>(CHUNK_SIZE, service);
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public AtCoderStep1Processor step1Processor(@Value("#{jobExecutionContext['atcoder.sourceId']}") String sourceId) {
        return new AtCoderStep1Processor(sourceId);
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_WRITER)
    @StepScope
    public AtCoderStep1Writer step1Writer() {
        return new AtCoderStep1Writer(contestInternalApi);
    }
}
