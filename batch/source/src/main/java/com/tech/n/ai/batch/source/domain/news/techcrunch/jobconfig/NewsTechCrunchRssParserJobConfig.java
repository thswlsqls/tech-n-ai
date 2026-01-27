package com.tech.n.ai.batch.source.domain.news.techcrunch.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.techcrunch.incrementer.NewsTechCrunchIncrementer;
import com.tech.n.ai.batch.source.domain.news.techcrunch.jobparameter.NewsTechCrunchJobParameter;
import com.tech.n.ai.batch.source.domain.news.techcrunch.listener.TechCrunchJobListener;
import com.tech.n.ai.batch.source.domain.news.techcrunch.processor.TechCrunchStep1Processor;
import com.tech.n.ai.batch.source.domain.news.techcrunch.reader.TechCrunchRssItemReader;
import com.tech.n.ai.batch.source.domain.news.techcrunch.service.NewsTechCrunchRssService;
import com.tech.n.ai.batch.source.domain.news.techcrunch.writer.TechCrunchStep1Writer;
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
public class NewsTechCrunchRssParserJobConfig {

    private static final String JOB_NAME = Constants.NEWS_TECHCRUNCH;
    private static final String STEP_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final NewsTechCrunchRssService service;
    private final NewsInternalContract newsInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name = JOB_NAME + Constants.PARAMETER)
    @JobScope
    public NewsTechCrunchJobParameter parameter() {
        return new NewsTechCrunchJobParameter();
    }

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository, 
                   @Qualifier(STEP_NAME) Step step,
                   @Qualifier(JOB_NAME + ".listener") TechCrunchJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .incrementer(new NewsTechCrunchIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = JOB_NAME + ".listener")
    public TechCrunchJobListener jobListener() {
        return new TechCrunchJobListener(redisTemplate);
    }

    @Bean(name = STEP_NAME)
    @JobScope
    public Step step(JobRepository jobRepository,
                     @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                     @Qualifier(STEP_NAME + Constants.ITEM_READER) TechCrunchRssItemReader reader,
                     @Qualifier(STEP_NAME + Constants.ITEM_PROCESSOR) TechCrunchStep1Processor processor,
                     @Qualifier(STEP_NAME + Constants.ITEM_WRITER) TechCrunchStep1Writer writer) {
        return new StepBuilder(STEP_NAME, jobRepository)
            .<RssFeedItem, NewsCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP_NAME + Constants.ITEM_READER)
    @StepScope
    public TechCrunchRssItemReader reader() {
        return new TechCrunchRssItemReader(CHUNK_SIZE, service);
    }

    @Bean(name = STEP_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public TechCrunchStep1Processor processor(@Value("#{jobExecutionContext['techcrunch.sourceId']}") String sourceId) {
        return new TechCrunchStep1Processor(sourceId);
    }

    @Bean(name = STEP_NAME + Constants.ITEM_WRITER)
    @StepScope
    public TechCrunchStep1Writer writer() {
        return new TechCrunchStep1Writer(newsInternalApi);
    }
}
