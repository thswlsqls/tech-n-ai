package com.tech.n.ai.batch.source.domain.news.techcrunch.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.techcrunch.incrementer.NewsTechCrunchIncrementer;
import com.tech.n.ai.batch.source.domain.news.techcrunch.jobparameter.NewsTechCrunchJobParameter;
import com.tech.n.ai.batch.source.domain.news.techcrunch.processor.TechCrunchStep1Processor;
import com.tech.n.ai.batch.source.domain.news.techcrunch.reader.TechCrunchRssItemReader;
import com.tech.n.ai.batch.source.domain.news.techcrunch.service.NewsTechCrunchRssService;
import com.tech.n.ai.batch.source.domain.news.techcrunch.writer.TechCrunchStep1Writer;
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
public class NewsTechCrunchRssParserJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final NewsTechCrunchRssService service;
    private final NewsTechCrunchJobParameter parameter;
    private final NewsInternalContract newsInternalApi;

    @Bean(name=Constants.NEWS_TECHCRUNCH + Constants.PARAMETER)
    @JobScope
    public NewsTechCrunchJobParameter parameter() { 
        return new NewsTechCrunchJobParameter(); 
    }

    @Bean(name=Constants.NEWS_TECHCRUNCH)
    public Job NewsTechCrunchJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_TECHCRUNCH + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.NEWS_TECHCRUNCH, jobRepository)
            .start(step1)
            .incrementer(new NewsTechCrunchIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_READER) TechCrunchRssItemReader<RssFeedItem> reader,
                      @Qualifier(Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_PROCESSOR) TechCrunchStep1Processor processor,
                      @Qualifier(Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_WRITER) TechCrunchStep1Writer writer) {

        return new StepBuilder(Constants.NEWS_TECHCRUNCH + Constants.STEP_1, jobRepository)
            .<RssFeedItem, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public TechCrunchRssItemReader<RssFeedItem> step1Reader() {
        return new TechCrunchRssItemReader<RssFeedItem>(
            Constants.CHUNK_SIZE_10, 
            service);
    }

    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public TechCrunchStep1Processor step1Processor() {
        return new TechCrunchStep1Processor();
    }

    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public TechCrunchStep1Writer step1Writer() {
        return new TechCrunchStep1Writer(newsInternalApi);
    }

    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
