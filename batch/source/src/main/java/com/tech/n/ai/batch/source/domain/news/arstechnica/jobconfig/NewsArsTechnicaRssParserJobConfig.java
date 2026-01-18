package com.tech.n.ai.batch.source.domain.news.arstechnica.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.arstechnica.incrementer.NewsArsTechnicaIncrementer;
import com.tech.n.ai.batch.source.domain.news.arstechnica.jobparameter.NewsArsTechnicaJobParameter;
import com.tech.n.ai.batch.source.domain.news.arstechnica.processor.ArsTechnicaStep1Processor;
import com.tech.n.ai.batch.source.domain.news.arstechnica.reader.ArsTechnicaRssItemReader;
import com.tech.n.ai.batch.source.domain.news.arstechnica.service.NewsArsTechnicaRssService;
import com.tech.n.ai.batch.source.domain.news.arstechnica.writer.ArsTechnicaStep1Writer;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
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
public class NewsArsTechnicaRssParserJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final NewsArsTechnicaRssService service;
    private final NewsArsTechnicaJobParameter parameter;
    private final NewsInternalContract newsInternalApi;

    @Bean(name=Constants.NEWS_ARS_TECHNICA + Constants.PARAMETER)
    @JobScope
    public NewsArsTechnicaJobParameter parameter() { 
        return new NewsArsTechnicaJobParameter(); 
    }

    @Bean(name=Constants.NEWS_ARS_TECHNICA)
    public Job NewsArsTechnicaJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_ARS_TECHNICA + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.NEWS_ARS_TECHNICA, jobRepository)
            .start(step1)
            .incrementer(new NewsArsTechnicaIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.NEWS_ARS_TECHNICA + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.NEWS_ARS_TECHNICA + Constants.STEP_1 + Constants.ITEM_READER) ArsTechnicaRssItemReader<RssFeedItem> reader,
                      @Qualifier(Constants.NEWS_ARS_TECHNICA + Constants.STEP_1 + Constants.ITEM_PROCESSOR) ArsTechnicaStep1Processor processor,
                      @Qualifier(Constants.NEWS_ARS_TECHNICA + Constants.STEP_1 + Constants.ITEM_WRITER) ArsTechnicaStep1Writer writer) {

        return new StepBuilder(Constants.NEWS_ARS_TECHNICA + Constants.STEP_1, jobRepository)
            .<RssFeedItem, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.NEWS_ARS_TECHNICA + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public ArsTechnicaRssItemReader<RssFeedItem> step1Reader() {
        return new ArsTechnicaRssItemReader<RssFeedItem>(
            Constants.CHUNK_SIZE_10, 
            service);
    }

    @Bean(name = Constants.NEWS_ARS_TECHNICA + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public ArsTechnicaStep1Processor step1Processor() {
        return new ArsTechnicaStep1Processor();
    }

    @Bean(name = Constants.NEWS_ARS_TECHNICA + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public ArsTechnicaStep1Writer step1Writer() {
        return new ArsTechnicaStep1Writer(newsInternalApi);
    }

    @Bean(name = Constants.NEWS_ARS_TECHNICA + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
