package com.tech.n.ai.batch.source.domain.news.medium.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.medium.incrementer.NewsMediumIncrementer;
import com.tech.n.ai.batch.source.domain.news.medium.jobparameter.NewsMediumJobParameter;
import com.tech.n.ai.batch.source.domain.news.medium.listener.MediumJobListener;
import com.tech.n.ai.batch.source.domain.news.medium.processor.MediumStep1Processor;
import com.tech.n.ai.batch.source.domain.news.medium.reader.MediumRssItemReader;
import com.tech.n.ai.batch.source.domain.news.medium.service.NewsMediumRssService;
import com.tech.n.ai.batch.source.domain.news.medium.writer.MediumStep1Writer;
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
public class NewsMediumRssParserJobConfig {

    private static final String JOB_NAME = Constants.NEWS_MEDIUM;
    private static final String STEP_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final NewsMediumRssService service;
    private final NewsInternalContract newsInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name = JOB_NAME + Constants.PARAMETER)
    @JobScope
    public NewsMediumJobParameter parameter() {
        return new NewsMediumJobParameter();
    }

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository, 
                   @Qualifier(STEP_NAME) Step step,
                   @Qualifier(JOB_NAME + ".listener") MediumJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .incrementer(new NewsMediumIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = JOB_NAME + ".listener")
    public MediumJobListener jobListener() {
        return new MediumJobListener(redisTemplate);
    }

    @Bean(name = STEP_NAME)
    @JobScope
    public Step step(JobRepository jobRepository,
                     @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                     @Qualifier(STEP_NAME + Constants.ITEM_READER) MediumRssItemReader reader,
                     @Qualifier(STEP_NAME + Constants.ITEM_PROCESSOR) MediumStep1Processor processor,
                     @Qualifier(STEP_NAME + Constants.ITEM_WRITER) MediumStep1Writer writer) {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<RssFeedItem, NewsCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP_NAME + Constants.ITEM_READER)
    @StepScope
    public MediumRssItemReader reader() {
        return new MediumRssItemReader(CHUNK_SIZE, service);
    }

    @Bean(name = STEP_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public MediumStep1Processor processor(@Value("#{jobExecutionContext['medium.sourceId']}") String sourceId) {
        return new MediumStep1Processor(sourceId);
    }

    @Bean(name = STEP_NAME + Constants.ITEM_WRITER)
    @StepScope
    public MediumStep1Writer writer() {
        return new MediumStep1Writer(newsInternalApi);
    }
}
