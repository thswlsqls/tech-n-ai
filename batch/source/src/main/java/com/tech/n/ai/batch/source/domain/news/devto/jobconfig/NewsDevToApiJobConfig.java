package com.tech.n.ai.batch.source.domain.news.devto.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.batch.source.domain.news.devto.incrementer.NewsDevToIncrementer;
import com.tech.n.ai.batch.source.domain.news.devto.jobparameter.NewsDevToJobParameter;
import com.tech.n.ai.batch.source.domain.news.devto.processor.NewsDevToStep1Processor;
import com.tech.n.ai.batch.source.domain.news.devto.reader.NewsDevToApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.news.devto.service.NewsDevToApiService;
import com.tech.n.ai.batch.source.domain.news.devto.writer.NewsDevToStep1Writer;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.Article;
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
public class NewsDevToApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${tag:#{null}}")
    private String tag;

    @Value("${username:#{null}}")
    private String username;

    @Value("${state:#{null}}")
    private String state;

    @Value("${top:#{null}}")
    private String top;

    @Value("${collection_id:#{null}}")
    private String collectionId;

    @Value("${page:#{null}}")
    private String page;

    @Value("${per_page:#{null}}")
    private String perPage;

    private final NewsDevToApiService service;
    private final NewsDevToJobParameter parameter;
    private final NewsInternalContract newsInternalApi;

    @Bean(name=Constants.NEWS_DEVTO + Constants.PARAMETER)
    @JobScope
    public NewsDevToJobParameter parameter() { 
        return new NewsDevToJobParameter(); 
    }

    @Bean(name=Constants.NEWS_DEVTO)
    public Job NewsDevToJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_DEVTO + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.NEWS_DEVTO, jobRepository)
            .start(step1)
            .incrementer(new NewsDevToIncrementer(baseDate, tag, username, state, top, collectionId, page, perPage))
            .build();
    }

    @Bean(name = Constants.NEWS_DEVTO + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.NEWS_DEVTO + Constants.STEP_1 + Constants.ITEM_READER) NewsDevToApiPagingItemReader<Article> reader,
                      @Qualifier(Constants.NEWS_DEVTO + Constants.STEP_1 + Constants.ITEM_PROCESSOR) NewsDevToStep1Processor processor,
                      @Qualifier(Constants.NEWS_DEVTO + Constants.STEP_1 + Constants.ITEM_WRITER) NewsDevToStep1Writer writer) {

        return new StepBuilder(Constants.NEWS_DEVTO + Constants.STEP_1, jobRepository)
            .<Article, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.NEWS_DEVTO + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public NewsDevToApiPagingItemReader<Article> step1Reader(
            @Value("#{jobParameters['tag']}") String tag,
            @Value("#{jobParameters['username']}") String username,
            @Value("#{jobParameters['state']}") String state,
            @Value("#{jobParameters['top']}") String top,
            @Value("#{jobParameters['collection_id']}") String collectionId,
            @Value("#{jobParameters['page']}") String page,
            @Value("#{jobParameters['per_page']}") String perPage) {
        Integer topValue = (top != null && !top.isBlank()) 
            ? Integer.parseInt(top) 
            : null;
        Integer collectionIdValue = (collectionId != null && !collectionId.isBlank()) 
            ? Integer.parseInt(collectionId) 
            : null;
        Integer pageValue = (page != null && !page.isBlank()) 
            ? Integer.parseInt(page) 
            : null;
        Integer perPageValue = (perPage != null && !perPage.isBlank()) 
            ? Integer.parseInt(perPage) 
            : null;
        
        return new NewsDevToApiPagingItemReader<Article>(
            Constants.CHUNK_SIZE_10, 
            service,
            tag,
            username,
            state,
            topValue,
            collectionIdValue,
            pageValue,
            perPageValue);
    }

    @Bean(name = Constants.NEWS_DEVTO + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public NewsDevToStep1Processor step1Processor() {
        return new NewsDevToStep1Processor();
    }

    @Bean(name = Constants.NEWS_DEVTO + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public NewsDevToStep1Writer step1Writer() {
        return new NewsDevToStep1Writer(newsInternalApi);
    }

    @Bean(name = Constants.NEWS_DEVTO + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
