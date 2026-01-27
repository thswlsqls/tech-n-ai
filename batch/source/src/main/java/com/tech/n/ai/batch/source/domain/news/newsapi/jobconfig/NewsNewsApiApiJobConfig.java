package com.tech.n.ai.batch.source.domain.news.newsapi.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.newsapi.incrementer.NewsNewsApiIncrementer;
import com.tech.n.ai.batch.source.domain.news.newsapi.jobparameter.NewsNewsApiJobParameter;
import com.tech.n.ai.batch.source.domain.news.newsapi.processor.NewsApiStep1Processor;
import com.tech.n.ai.batch.source.domain.news.newsapi.reader.NewsApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.news.newsapi.service.NewsApiService;
import com.tech.n.ai.batch.source.domain.news.newsapi.writer.NewsApiStep1Writer;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.tech.n.ai.client.feign.domain.newsapi.contract.NewsAPIDto.Article;
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
public class NewsNewsApiApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${query:#{null}}")
    private String query;

    @Value("${sources:#{null}}")
    private String sources;

    @Value("${domains:#{null}}")
    private String domains;

    @Value("${excludeDomains:#{null}}")
    private String excludeDomains;

    @Value("${from:#{null}}")
    private String from;

    @Value("${to:#{null}}")
    private String to;

    @Value("${language:#{null}}")
    private String language;

    @Value("${sortBy:#{null}}")
    private String sortBy;

    @Value("${pageSize:#{null}}")
    private String pageSize;

    @Value("${page:#{null}}")
    private String page;

    private final NewsApiService service;
    private final NewsNewsApiJobParameter parameter;
    private final NewsInternalContract newsInternalApi;
    private final RedisTemplate<String, String> redisTemplate;


    @Bean(name=Constants.NEWS_NEWSAPI + Constants.PARAMETER)
    @JobScope
    public NewsNewsApiJobParameter parameter() { 
        return new NewsNewsApiJobParameter(); 
    }

    @Bean(name=Constants.NEWS_NEWSAPI)
    public Job NewsNewsApiJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_NEWSAPI + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.NEWS_NEWSAPI, jobRepository)
            .start(step1)
            .incrementer(new NewsNewsApiIncrementer(baseDate, query, sources, domains, excludeDomains, from, to, language, sortBy, pageSize, page))
            .build();
    }

    @Bean(name = Constants.NEWS_NEWSAPI + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(Constants.NEWS_NEWSAPI + Constants.STEP_1 + Constants.ITEM_READER) NewsApiPagingItemReader<Article> reader,
                      @Qualifier(Constants.NEWS_NEWSAPI + Constants.STEP_1 + Constants.ITEM_PROCESSOR) NewsApiStep1Processor processor,
                      @Qualifier(Constants.NEWS_NEWSAPI + Constants.STEP_1 + Constants.ITEM_WRITER) NewsApiStep1Writer writer) {

        return new StepBuilder(Constants.NEWS_NEWSAPI + Constants.STEP_1, jobRepository)
            .<Article, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.NEWS_NEWSAPI + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public NewsApiPagingItemReader<Article> step1Reader(
            @Value("#{jobParameters['query']}") String query,
            @Value("#{jobParameters['sources']}") String sources,
            @Value("#{jobParameters['domains']}") String domains,
            @Value("#{jobParameters['excludeDomains']}") String excludeDomains,
            @Value("#{jobParameters['from']}") String from,
            @Value("#{jobParameters['to']}") String to,
            @Value("#{jobParameters['language']}") String language,
            @Value("#{jobParameters['sortBy']}") String sortBy,
            @Value("#{jobParameters['pageSize']}") String pageSize,
            @Value("#{jobParameters['page']}") String page) {
        Integer pageSizeValue = (pageSize != null && !pageSize.isBlank()) 
            ? Integer.parseInt(pageSize) 
            : null;
        Integer pageValue = (page != null && !page.isBlank()) 
            ? Integer.parseInt(page) 
            : null;
        
        return new NewsApiPagingItemReader<Article>(
            Constants.CHUNK_SIZE_10, 
            service,
            query,
            sources,
            domains,
            excludeDomains,
            from,
            to,
            language,
            sortBy,
            pageSizeValue,
            pageValue);
    }

    @Bean(name = Constants.NEWS_NEWSAPI + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public NewsApiStep1Processor step1Processor() {
        return new NewsApiStep1Processor(redisTemplate);
    }

    @Bean(name = Constants.NEWS_NEWSAPI + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public NewsApiStep1Writer step1Writer() {
        return new NewsApiStep1Writer(newsInternalApi);
    }

    @Bean(name = Constants.NEWS_NEWSAPI + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
