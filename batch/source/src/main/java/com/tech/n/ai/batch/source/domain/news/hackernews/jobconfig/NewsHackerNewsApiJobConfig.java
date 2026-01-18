package com.tech.n.ai.batch.source.domain.news.hackernews.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.hackernews.incrementer.NewsHackerNewsIncrementer;
import com.tech.n.ai.batch.source.domain.news.hackernews.jobparameter.NewsHackerNewsJobParameter;
import com.tech.n.ai.batch.source.domain.news.hackernews.processor.NewsHackerNewsStep1Processor;
import com.tech.n.ai.batch.source.domain.news.hackernews.reader.NewsHackerNewsApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.news.hackernews.service.NewsHackerNewsApiService;
import com.tech.n.ai.batch.source.domain.news.hackernews.writer.NewsHackerNewsStep1Writer;
import com.tech.n.ai.client.feign.domain.hackernews.contract.HackerNewsDto.ItemResponse;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
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
public class NewsHackerNewsApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${limit:#{null}}")
    private String limit;

    private final NewsHackerNewsApiService service;
    private final NewsHackerNewsJobParameter parameter;
    private final NewsInternalContract newsInternalApi;

    @Bean(name=Constants.NEWS_HACKERNEWS + Constants.PARAMETER)
    @JobScope
    public NewsHackerNewsJobParameter parameter() { 
        return new NewsHackerNewsJobParameter(); 
    }

    @Bean(name=Constants.NEWS_HACKERNEWS)
    public Job NewsHackerNewsJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_HACKERNEWS + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.NEWS_HACKERNEWS, jobRepository)
            .start(step1)
            .incrementer(new NewsHackerNewsIncrementer(baseDate, limit))
            .build();
    }

    @Bean(name = Constants.NEWS_HACKERNEWS + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.NEWS_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_READER) NewsHackerNewsApiPagingItemReader<ItemResponse> reader,
                      @Qualifier(Constants.NEWS_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_PROCESSOR) NewsHackerNewsStep1Processor processor,
                      @Qualifier(Constants.NEWS_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_WRITER) NewsHackerNewsStep1Writer writer) {

        return new StepBuilder(Constants.NEWS_HACKERNEWS + Constants.STEP_1, jobRepository)
            .<ItemResponse, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.NEWS_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public NewsHackerNewsApiPagingItemReader<ItemResponse> step1Reader(
            @Value("#{jobParameters['limit']}") String limit) {
        Integer limitValue = (limit != null && !limit.isBlank()) 
            ? Integer.parseInt(limit) 
            : null;
        
        return new NewsHackerNewsApiPagingItemReader<ItemResponse>(
            Constants.CHUNK_SIZE_10, 
            service,
            limitValue);
    }

    @Bean(name = Constants.NEWS_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public NewsHackerNewsStep1Processor step1Processor() {
        return new NewsHackerNewsStep1Processor();
    }

    @Bean(name = Constants.NEWS_HACKERNEWS + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public NewsHackerNewsStep1Writer step1Writer() {
        return new NewsHackerNewsStep1Writer(newsInternalApi);
    }

    @Bean(name = Constants.NEWS_HACKERNEWS + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
