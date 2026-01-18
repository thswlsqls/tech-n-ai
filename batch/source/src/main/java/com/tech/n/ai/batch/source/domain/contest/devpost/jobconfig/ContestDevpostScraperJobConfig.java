package com.tech.n.ai.batch.source.domain.contest.devpost.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.batch.source.domain.contest.devpost.incrementer.ContestDevpostIncrementer;
import com.tech.n.ai.batch.source.domain.contest.devpost.jobparameter.ContestDevpostJobParameter;
import com.tech.n.ai.batch.source.domain.contest.devpost.processor.DevpostStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.devpost.reader.DevpostScrapingItemReader;
import com.tech.n.ai.batch.source.domain.contest.devpost.service.ContestDevpostScraperService;
import com.tech.n.ai.batch.source.domain.contest.devpost.writer.DevpostStep1Writer;
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
public class ContestDevpostScraperJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final ContestDevpostScraperService service;
    private final ContestDevpostJobParameter parameter;
    private final ContestInternalContract contestInternalApi;

    @Bean(name=Constants.CONTEST_DEVPOST + Constants.PARAMETER)
    @JobScope
    public ContestDevpostJobParameter parameter() { 
        return new ContestDevpostJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_DEVPOST)
    public Job ContestDevpostJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_DEVPOST + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_DEVPOST, jobRepository)
            .start(step1)
            .incrementer(new ContestDevpostIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.CONTEST_DEVPOST + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_DEVPOST + Constants.STEP_1 + Constants.ITEM_READER) DevpostScrapingItemReader<ScrapedContestItem> reader,
                      @Qualifier(Constants.CONTEST_DEVPOST + Constants.STEP_1 + Constants.ITEM_PROCESSOR) DevpostStep1Processor processor,
                      @Qualifier(Constants.CONTEST_DEVPOST + Constants.STEP_1 + Constants.ITEM_WRITER) DevpostStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_DEVPOST + Constants.STEP_1, jobRepository)
            .<ScrapedContestItem, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_DEVPOST + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public DevpostScrapingItemReader<ScrapedContestItem> step1Reader() {
        return new DevpostScrapingItemReader<ScrapedContestItem>(
            Constants.CHUNK_SIZE_10, 
            service);
    }

    @Bean(name = Constants.CONTEST_DEVPOST + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public DevpostStep1Processor step1Processor() {
        return new DevpostStep1Processor();
    }

    @Bean(name = Constants.CONTEST_DEVPOST + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public DevpostStep1Writer step1Writer() {
        return new DevpostStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_DEVPOST + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
