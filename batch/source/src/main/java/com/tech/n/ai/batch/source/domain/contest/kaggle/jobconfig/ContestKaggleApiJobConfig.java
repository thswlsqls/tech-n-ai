package com.tech.n.ai.batch.source.domain.contest.kaggle.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.kaggle.incrementer.ContestKaggleIncrementer;
import com.tech.n.ai.batch.source.domain.contest.kaggle.jobparameter.ContestKaggleJobParameter;
import com.tech.n.ai.batch.source.domain.contest.kaggle.processor.KaggleStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.kaggle.reader.KaggleApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.kaggle.service.KaggleApiService;
import com.tech.n.ai.batch.source.domain.contest.kaggle.writer.KaggleStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.feign.domain.kaggle.contract.KaggleDto.Competition;
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
public class ContestKaggleApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${page:#{null}}")
    private String page;

    @Value("${search:#{null}}")
    private String search;

    @Value("${category:#{null}}")
    private String category;

    @Value("${sortBy:#{null}}")
    private String sortBy;

    @Value("${group:#{null}}")
    private String group;

    @Value("${filter:#{null}}")
    private String filter;

    private final KaggleApiService service;
    private final ContestKaggleJobParameter parameter;
    private final ContestInternalContract contestInternalApi;

    @Bean(name=Constants.CONTEST_KAGGLE + Constants.PARAMETER)
    @JobScope
    public ContestKaggleJobParameter parameter() { 
        return new ContestKaggleJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_KAGGLE)
    public Job ContestKaggleJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_KAGGLE + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_KAGGLE, jobRepository)
            .start(step1)
            .incrementer(new ContestKaggleIncrementer(baseDate, page, search, category, sortBy, group, filter))
            .build();
    }

    @Bean(name = Constants.CONTEST_KAGGLE + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_KAGGLE + Constants.STEP_1 + Constants.ITEM_READER) KaggleApiPagingItemReader<Competition> reader,
                      @Qualifier(Constants.CONTEST_KAGGLE + Constants.STEP_1 + Constants.ITEM_PROCESSOR) KaggleStep1Processor processor,
                      @Qualifier(Constants.CONTEST_KAGGLE + Constants.STEP_1 + Constants.ITEM_WRITER) KaggleStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_KAGGLE + Constants.STEP_1, jobRepository)
            .<Competition, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_KAGGLE + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public KaggleApiPagingItemReader<Competition> step1Reader(
            @Value("#{jobParameters['page']}") String page,
            @Value("#{jobParameters['search']}") String search,
            @Value("#{jobParameters['category']}") String category,
            @Value("#{jobParameters['sortBy']}") String sortBy,
            @Value("#{jobParameters['group']}") String group,
            @Value("#{jobParameters['filter']}") String filter) {
        Integer pageValue = (page != null && !page.isBlank()) 
            ? Integer.parseInt(page) 
            : null;
        
        return new KaggleApiPagingItemReader<Competition>(
            Constants.CHUNK_SIZE_10, 
            service,
            pageValue,
            search,
            category,
            sortBy,
            group,
            filter);
    }

    @Bean(name = Constants.CONTEST_KAGGLE + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public KaggleStep1Processor step1Processor() {
        return new KaggleStep1Processor();
    }

    @Bean(name = Constants.CONTEST_KAGGLE + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public KaggleStep1Writer step1Writer() {
        return new KaggleStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_KAGGLE + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
