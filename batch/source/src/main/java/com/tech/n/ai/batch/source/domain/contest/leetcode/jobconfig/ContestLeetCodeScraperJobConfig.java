package com.tech.n.ai.batch.source.domain.contest.leetcode.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.batch.source.domain.contest.leetcode.incrementer.ContestLeetCodeIncrementer;
import com.tech.n.ai.batch.source.domain.contest.leetcode.jobparameter.ContestLeetCodeJobParameter;
import com.tech.n.ai.batch.source.domain.contest.leetcode.processor.LeetCodeStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.leetcode.reader.LeetCodeScrapingItemReader;
import com.tech.n.ai.batch.source.domain.contest.leetcode.service.ContestLeetCodeScraperService;
import com.tech.n.ai.batch.source.domain.contest.leetcode.writer.LeetCodeStep1Writer;
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
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestLeetCodeScraperJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final ContestLeetCodeScraperService service;
    private final ContestLeetCodeJobParameter parameter;
    private final ContestInternalContract contestInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name=Constants.CONTEST_LEETCODE + Constants.PARAMETER)
    @JobScope
    public ContestLeetCodeJobParameter parameter() { 
        return new ContestLeetCodeJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_LEETCODE)
    public Job ContestLeetCodeJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_LEETCODE + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_LEETCODE, jobRepository)
            .start(step1)
            .incrementer(new ContestLeetCodeIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_READER) LeetCodeScrapingItemReader<ScrapedContestItem> reader,
                      @Qualifier(Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_PROCESSOR) LeetCodeStep1Processor processor,
                      @Qualifier(Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_WRITER) LeetCodeStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_LEETCODE + Constants.STEP_1, jobRepository)
            .<ScrapedContestItem, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public LeetCodeScrapingItemReader<ScrapedContestItem> step1Reader() {
        return new LeetCodeScrapingItemReader<ScrapedContestItem>(
            Constants.CHUNK_SIZE_10, 
            service);
    }

    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public LeetCodeStep1Processor step1Processor() {
        return new LeetCodeStep1Processor(redisTemplate);
    }

    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public LeetCodeStep1Writer step1Writer() {
        return new LeetCodeStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_LEETCODE + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
