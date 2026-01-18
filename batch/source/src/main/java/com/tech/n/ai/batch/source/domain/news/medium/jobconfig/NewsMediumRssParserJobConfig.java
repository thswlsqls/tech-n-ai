package com.tech.n.ai.batch.source.domain.news.medium.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.medium.incrementer.NewsMediumIncrementer;
import com.tech.n.ai.batch.source.domain.news.medium.jobparameter.NewsMediumJobParameter;
import com.tech.n.ai.batch.source.domain.news.medium.processor.MediumStep1Processor;
import com.tech.n.ai.batch.source.domain.news.medium.reader.MediumRssItemReader;
import com.tech.n.ai.batch.source.domain.news.medium.service.NewsMediumRssService;
import com.tech.n.ai.batch.source.domain.news.medium.writer.MediumStep1Writer;
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
public class NewsMediumRssParserJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final NewsMediumRssService service;
    private final NewsMediumJobParameter parameter;
    private final NewsInternalContract newsInternalApi;

    @Bean(name=Constants.NEWS_MEDIUM + Constants.PARAMETER)
    @JobScope
    public NewsMediumJobParameter parameter() { 
        return new NewsMediumJobParameter(); 
    }

    @Bean(name=Constants.NEWS_MEDIUM)
    public Job NewsMediumJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_MEDIUM + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.NEWS_MEDIUM, jobRepository)
            .start(step1)
            .incrementer(new NewsMediumIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.NEWS_MEDIUM + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.NEWS_MEDIUM + Constants.STEP_1 + Constants.ITEM_READER) MediumRssItemReader<RssFeedItem> reader,
                      @Qualifier(Constants.NEWS_MEDIUM + Constants.STEP_1 + Constants.ITEM_PROCESSOR) MediumStep1Processor processor,
                      @Qualifier(Constants.NEWS_MEDIUM + Constants.STEP_1 + Constants.ITEM_WRITER) MediumStep1Writer writer) {

        return new StepBuilder(Constants.NEWS_MEDIUM + Constants.STEP_1, jobRepository)
            .<RssFeedItem, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.NEWS_MEDIUM + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public MediumRssItemReader<RssFeedItem> step1Reader() {
        return new MediumRssItemReader<RssFeedItem>(
            Constants.CHUNK_SIZE_10, 
            service);
    }

    @Bean(name = Constants.NEWS_MEDIUM + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public MediumStep1Processor step1Processor() {
        return new MediumStep1Processor();
    }

    @Bean(name = Constants.NEWS_MEDIUM + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public MediumStep1Writer step1Writer() {
        return new MediumStep1Writer(newsInternalApi);
    }

    @Bean(name = Constants.NEWS_MEDIUM + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
