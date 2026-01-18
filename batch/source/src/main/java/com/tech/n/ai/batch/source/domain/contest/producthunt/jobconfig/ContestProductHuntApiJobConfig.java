package com.tech.n.ai.batch.source.domain.contest.producthunt.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.producthunt.incrementer.ContestProductHuntIncrementer;
import com.tech.n.ai.batch.source.domain.contest.producthunt.jobparameter.ContestProductHuntJobParameter;
import com.tech.n.ai.batch.source.domain.contest.producthunt.processor.ProductHuntStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.producthunt.reader.ProductHuntApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.producthunt.service.ProductHuntApiService;
import com.tech.n.ai.batch.source.domain.contest.producthunt.writer.ProductHuntStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import java.util.Map;
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
public class ContestProductHuntApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${first:#{null}}")
    private String first;

    @Value("${after:#{null}}")
    private String after;

    private final ProductHuntApiService service;
    private final ContestProductHuntJobParameter parameter;
    private final ContestInternalContract contestInternalApi;

    @Bean(name=Constants.CONTEST_PRODUCTHUNT + Constants.PARAMETER)
    @JobScope
    public ContestProductHuntJobParameter parameter() { 
        return new ContestProductHuntJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_PRODUCTHUNT)
    public Job ContestProductHuntJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_PRODUCTHUNT, jobRepository)
            .start(step1)
            .incrementer(new ContestProductHuntIncrementer(baseDate, first, after))
            .build();
    }

    @Bean(name = Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1 + Constants.ITEM_READER) ProductHuntApiPagingItemReader<Map<String, Object>> reader,
                      @Qualifier(Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1 + Constants.ITEM_PROCESSOR) ProductHuntStep1Processor processor,
                      @Qualifier(Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1 + Constants.ITEM_WRITER) ProductHuntStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1, jobRepository)
            .<Map<String, Object>, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public ProductHuntApiPagingItemReader<Map<String, Object>> step1Reader(
            @Value("#{jobParameters['first']}") String first,
            @Value("#{jobParameters['after']}") String after) {
        Integer firstValue = (first != null && !first.isBlank()) 
            ? Integer.parseInt(first) 
            : null;
        
        return new ProductHuntApiPagingItemReader<Map<String, Object>>(
            Constants.CHUNK_SIZE_10, 
            service,
            firstValue,
            after);
    }

    @Bean(name = Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public ProductHuntStep1Processor step1Processor() {
        return new ProductHuntStep1Processor();
    }

    @Bean(name = Constants.CONTEST_PRODUCTHUNT + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public ProductHuntStep1Writer step1Writer() {
        return new ProductHuntStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_PRODUCTHUNT + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
