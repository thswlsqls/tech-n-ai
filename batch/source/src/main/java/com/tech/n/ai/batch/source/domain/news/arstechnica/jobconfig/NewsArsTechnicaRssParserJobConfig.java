package com.tech.n.ai.batch.source.domain.news.arstechnica.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.arstechnica.incrementer.NewsArsTechnicaIncrementer;
import com.tech.n.ai.batch.source.domain.news.arstechnica.jobparameter.NewsArsTechnicaJobParameter;
import com.tech.n.ai.batch.source.domain.news.arstechnica.listener.ArsTechnicaJobListener;
import com.tech.n.ai.batch.source.domain.news.arstechnica.processor.ArsTechnicaStep1Processor;
import com.tech.n.ai.batch.source.domain.news.arstechnica.reader.ArsTechnicaRssItemReader;
import com.tech.n.ai.batch.source.domain.news.arstechnica.service.NewsArsTechnicaRssService;
import com.tech.n.ai.batch.source.domain.news.arstechnica.writer.ArsTechnicaStep1Writer;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NewsArsTechnicaRssParserJobConfig {

    private static final String JOB_NAME = Constants.NEWS_ARS_TECHNICA;
    private static final String STEP_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final NewsArsTechnicaRssService service;
    private final NewsInternalContract newsInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name = JOB_NAME + Constants.PARAMETER)
    @JobScope
    public NewsArsTechnicaJobParameter parameter() {
        return new NewsArsTechnicaJobParameter();
    }

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository, 
                   @Qualifier(STEP_NAME) Step step,
                   @Qualifier(JOB_NAME + ".listener") ArsTechnicaJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .incrementer(new NewsArsTechnicaIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = JOB_NAME + ".listener")
    public ArsTechnicaJobListener jobListener() {
        return new ArsTechnicaJobListener(redisTemplate);
    }

    @Bean(name = STEP_NAME)
    @JobScope
    public Step step(JobRepository jobRepository,
                     @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                     @Qualifier(STEP_NAME + Constants.ITEM_READER) ArsTechnicaRssItemReader reader,
                     @Qualifier(STEP_NAME + Constants.ITEM_PROCESSOR) ArsTechnicaStep1Processor processor,
                     @Qualifier(STEP_NAME + Constants.ITEM_WRITER) ArsTechnicaStep1Writer writer) {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<RssFeedItem, NewsCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP_NAME + Constants.ITEM_READER)
    @StepScope
    public ArsTechnicaRssItemReader reader() {
        return new ArsTechnicaRssItemReader(CHUNK_SIZE, service);
    }

    @Bean(name = STEP_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public ArsTechnicaStep1Processor processor(@Value("#{jobExecutionContext['arstechnica.sourceId']}") String sourceId) {
        return new ArsTechnicaStep1Processor(sourceId);
    }

    @Bean(name = STEP_NAME + Constants.ITEM_WRITER)
    @StepScope
    public ArsTechnicaStep1Writer writer() {
        return new ArsTechnicaStep1Writer(newsInternalApi);
    }
}
