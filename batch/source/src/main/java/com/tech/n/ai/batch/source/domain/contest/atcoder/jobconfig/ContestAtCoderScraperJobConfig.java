package com.tech.n.ai.batch.source.domain.contest.atcoder.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.batch.source.domain.contest.atcoder.incrementer.ContestAtCoderIncrementer;
import com.tech.n.ai.batch.source.domain.contest.atcoder.jobparameter.ContestAtCoderJobParameter;
import com.tech.n.ai.batch.source.domain.contest.atcoder.processor.AtCoderStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.atcoder.reader.AtCoderScrapingItemReader;
import com.tech.n.ai.batch.source.domain.contest.atcoder.service.ContestAtCoderScraperService;
import com.tech.n.ai.batch.source.domain.contest.atcoder.writer.AtCoderStep1Writer;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
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
public class ContestAtCoderScraperJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final ContestAtCoderScraperService service;
    private final ContestAtCoderJobParameter parameter;
    private final ContestInternalContract contestInternalApi;

    @Bean(name=Constants.CONTEST_ATCODER + Constants.PARAMETER)
    @JobScope
    public ContestAtCoderJobParameter parameter() { 
        return new ContestAtCoderJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_ATCODER)
    public Job ContestAtCoderJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_ATCODER + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_ATCODER, jobRepository)
            .start(step1)
            .incrementer(new ContestAtCoderIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.CONTEST_ATCODER + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_ATCODER + Constants.STEP_1 + Constants.ITEM_READER) AtCoderScrapingItemReader<ScrapedContestItem> reader,
                      @Qualifier(Constants.CONTEST_ATCODER + Constants.STEP_1 + Constants.ITEM_PROCESSOR) AtCoderStep1Processor processor,
                      @Qualifier(Constants.CONTEST_ATCODER + Constants.STEP_1 + Constants.ITEM_WRITER) AtCoderStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_ATCODER + Constants.STEP_1, jobRepository)
            .<ScrapedContestItem, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_ATCODER + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public AtCoderScrapingItemReader<ScrapedContestItem> step1Reader() {
        return new AtCoderScrapingItemReader<ScrapedContestItem>(
            Constants.CHUNK_SIZE_10, 
            service);
    }

    @Bean(name = Constants.CONTEST_ATCODER + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public AtCoderStep1Processor step1Processor() {
        return new AtCoderStep1Processor();
    }

    @Bean(name = Constants.CONTEST_ATCODER + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public AtCoderStep1Writer step1Writer() {
        return new AtCoderStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_ATCODER + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
