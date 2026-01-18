package com.tech.n.ai.batch.source.domain.contest.gsoc.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.batch.source.domain.contest.gsoc.incrementer.ContestGSOCIncrementer;
import com.tech.n.ai.batch.source.domain.contest.gsoc.jobparameter.ContestGSOCJobParameter;
import com.tech.n.ai.batch.source.domain.contest.gsoc.processor.GSOCStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.gsoc.reader.GSOCScrapingItemReader;
import com.tech.n.ai.batch.source.domain.contest.gsoc.service.ContestGSOCScraperService;
import com.tech.n.ai.batch.source.domain.contest.gsoc.writer.GSOCStep1Writer;
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
public class ContestGSOCScraperJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final ContestGSOCScraperService service;
    private final ContestGSOCJobParameter parameter;
    private final ContestInternalContract contestInternalApi;

    @Bean(name=Constants.CONTEST_GSOC + Constants.PARAMETER)
    @JobScope
    public ContestGSOCJobParameter parameter() { 
        return new ContestGSOCJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_GSOC)
    public Job ContestGSOCJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_GSOC + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_GSOC, jobRepository)
            .start(step1)
            .incrementer(new ContestGSOCIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.CONTEST_GSOC + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_GSOC + Constants.STEP_1 + Constants.ITEM_READER) GSOCScrapingItemReader<ScrapedContestItem> reader,
                      @Qualifier(Constants.CONTEST_GSOC + Constants.STEP_1 + Constants.ITEM_PROCESSOR) GSOCStep1Processor processor,
                      @Qualifier(Constants.CONTEST_GSOC + Constants.STEP_1 + Constants.ITEM_WRITER) GSOCStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_GSOC + Constants.STEP_1, jobRepository)
            .<ScrapedContestItem, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_GSOC + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public GSOCScrapingItemReader<ScrapedContestItem> step1Reader() {
        return new GSOCScrapingItemReader<ScrapedContestItem>(
            Constants.CHUNK_SIZE_10, 
            service);
    }

    @Bean(name = Constants.CONTEST_GSOC + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public GSOCStep1Processor step1Processor() {
        return new GSOCStep1Processor();
    }

    @Bean(name = Constants.CONTEST_GSOC + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public GSOCStep1Writer step1Writer() {
        return new GSOCStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_GSOC + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
