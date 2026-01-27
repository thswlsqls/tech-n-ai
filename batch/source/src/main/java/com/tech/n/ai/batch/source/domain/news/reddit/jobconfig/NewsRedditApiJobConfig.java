package com.tech.n.ai.batch.source.domain.news.reddit.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.reddit.incrementer.NewsRedditIncrementer;
import com.tech.n.ai.batch.source.domain.news.reddit.jobparameter.NewsRedditJobParameter;
import com.tech.n.ai.batch.source.domain.news.reddit.processor.NewsRedditStep1Processor;
import com.tech.n.ai.batch.source.domain.news.reddit.reader.NewsRedditApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.news.reddit.service.NewsRedditApiService;
import com.tech.n.ai.batch.source.domain.news.reddit.writer.NewsRedditStep1Writer;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.Post;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NewsRedditApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${subreddit:#{null}}")
    private String subreddit;

    @Value("${limit:#{null}}")
    private String limit;

    @Value("${after:#{null}}")
    private String after;

    @Value("${before:#{null}}")
    private String before;

    private final NewsRedditApiService service;
    private final NewsRedditJobParameter parameter;
    private final NewsInternalContract newsInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name=Constants.NEWS_REDDIT + Constants.PARAMETER)
    @JobScope
    public NewsRedditJobParameter parameter() { 
        return new NewsRedditJobParameter(); 
    }

    @Bean(name=Constants.NEWS_REDDIT)
    public Job NewsRedditJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_REDDIT + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.NEWS_REDDIT, jobRepository)
            .start(step1)
            .incrementer(new NewsRedditIncrementer(baseDate, subreddit, limit, after, before))
            .build();
    }

    @Bean(name = Constants.NEWS_REDDIT + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(Constants.NEWS_REDDIT + Constants.STEP_1 + Constants.ITEM_READER) NewsRedditApiPagingItemReader<Post> reader,
                      @Qualifier(Constants.NEWS_REDDIT + Constants.STEP_1 + Constants.ITEM_PROCESSOR) NewsRedditStep1Processor processor,
                      @Qualifier(Constants.NEWS_REDDIT + Constants.STEP_1 + Constants.ITEM_WRITER) NewsRedditStep1Writer writer) {

        return new StepBuilder(Constants.NEWS_REDDIT + Constants.STEP_1, jobRepository)
            .<Post, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.NEWS_REDDIT + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public NewsRedditApiPagingItemReader<Post> step1Reader(
            @Value("#{jobParameters['subreddit']}") String subreddit,
            @Value("#{jobParameters['limit']}") String limit,
            @Value("#{jobParameters['after']}") String after,
            @Value("#{jobParameters['before']}") String before) {
        Integer limitValue = (limit != null && !limit.isBlank()) 
            ? Integer.parseInt(limit) 
            : null;
        
        return new NewsRedditApiPagingItemReader<Post>(
            Constants.CHUNK_SIZE_10, 
            service,
            subreddit,
            limitValue,
            after,
            before);
    }

    @Bean(name = Constants.NEWS_REDDIT + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public NewsRedditStep1Processor step1Processor() {
        return new NewsRedditStep1Processor(redisTemplate);
    }

    @Bean(name = Constants.NEWS_REDDIT + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public NewsRedditStep1Writer step1Writer() {
        return new NewsRedditStep1Writer(newsInternalApi);
    }

    @Bean(name = Constants.NEWS_REDDIT + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
