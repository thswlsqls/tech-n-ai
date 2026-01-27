package com.tech.n.ai.batch.source.domain.contest.devto.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.devto.incrementer.ContestDevToIncrementer;
import com.tech.n.ai.batch.source.domain.contest.devto.jobparameter.ContestDevToJobParameter;
import com.tech.n.ai.batch.source.domain.contest.devto.processor.DevToStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.devto.reader.DevToApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.devto.service.DevToApiService;
import com.tech.n.ai.batch.source.domain.contest.devto.writer.DevToStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.devto.contract.DevToDto.Article;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
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
public class ContestDevToApiJobConfig {

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

    private final DevToApiService service;
    private final ContestDevToJobParameter parameter;
    private final ContestInternalContract contestInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name=Constants.CONTEST_DEVTO + Constants.PARAMETER)
    @JobScope
    public ContestDevToJobParameter parameter() { 
        return new ContestDevToJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_DEVTO)
    public Job ContestDevToJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_DEVTO + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_DEVTO, jobRepository)
            .start(step1)
            .incrementer(new ContestDevToIncrementer(baseDate, tag, username, state, top, collectionId, page, perPage))
            .build();
    }

    @Bean(name = Constants.CONTEST_DEVTO + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(Constants.CONTEST_DEVTO + Constants.STEP_1 + Constants.ITEM_READER) DevToApiPagingItemReader<Article> reader,
                      @Qualifier(Constants.CONTEST_DEVTO + Constants.STEP_1 + Constants.ITEM_PROCESSOR) DevToStep1Processor processor,
                      @Qualifier(Constants.CONTEST_DEVTO + Constants.STEP_1 + Constants.ITEM_WRITER) DevToStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_DEVTO + Constants.STEP_1, jobRepository)
            .<Article, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_DEVTO + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public DevToApiPagingItemReader<Article> step1Reader(
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
        
        return new DevToApiPagingItemReader<Article>(
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

    @Bean(name = Constants.CONTEST_DEVTO + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public DevToStep1Processor step1Processor() {
        return new DevToStep1Processor(redisTemplate);
    }

    @Bean(name = Constants.CONTEST_DEVTO + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public DevToStep1Writer step1Writer() {
        return new DevToStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_DEVTO + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
