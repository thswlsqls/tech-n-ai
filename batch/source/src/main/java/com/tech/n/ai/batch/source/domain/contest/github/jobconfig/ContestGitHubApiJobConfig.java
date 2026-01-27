package com.tech.n.ai.batch.source.domain.contest.github.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.github.incrementer.ContestGitHubIncrementer;
import com.tech.n.ai.batch.source.domain.contest.github.jobparameter.ContestGitHubJobParameter;
import com.tech.n.ai.batch.source.domain.contest.github.listener.GitHubJobListener;
import com.tech.n.ai.batch.source.domain.contest.github.processor.GitHubStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.github.reader.GitHubApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.github.service.GitHubApiService;
import com.tech.n.ai.batch.source.domain.contest.github.writer.GitHubStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.Event;
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
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestGitHubApiJobConfig {

    private static final String JOB_NAME = Constants.CONTEST_GITHUB;
    private static final String STEP1_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;
    private static final Integer DEFAULT_PER_PAGE = 100;
    private static final Integer DEFAULT_PAGE = 1;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${perPage:1000}")
    private String perPage;

    @Value("${page:1}}")
    private String page;

    private final GitHubApiService service;
    private final ContestInternalContract contestInternalApi;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name = JOB_NAME + Constants.PARAMETER)
    @JobScope
    public ContestGitHubJobParameter parameter() {
        return new ContestGitHubJobParameter();
    }

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository,
                   @Qualifier(STEP1_NAME) Step step1,
                   @Qualifier(JOB_NAME + ".listener") GitHubJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step1)
            .incrementer(new ContestGitHubIncrementer(baseDate, perPage, page))
            .listener(listener)
            .build();
    }

    @Bean(name = JOB_NAME + ".listener")
    public GitHubJobListener jobListener() {
        return new GitHubJobListener(redisTemplate);
    }

    @Bean(name = STEP1_NAME)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP1_NAME + Constants.ITEM_READER) GitHubApiPagingItemReader reader,
                      @Qualifier(STEP1_NAME + Constants.ITEM_PROCESSOR) GitHubStep1Processor processor,
                      @Qualifier(STEP1_NAME + Constants.ITEM_WRITER) GitHubStep1Writer writer) {
        return new StepBuilder(STEP1_NAME, jobRepository)
            .<Event, ContestCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_READER)
    @StepScope
    public GitHubApiPagingItemReader step1Reader(
            @Value("#{jobParameters['perPage']}") String perPageParam,
            @Value("#{jobParameters['page']}") String pageParam) {
        Integer perPageValue = parseIntParameter(perPageParam, DEFAULT_PER_PAGE);
        Integer pageValue = parseIntParameter(pageParam, DEFAULT_PAGE);
        return new GitHubApiPagingItemReader(CHUNK_SIZE, service, perPageValue, pageValue);
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public GitHubStep1Processor step1Processor(@Value("#{jobExecutionContext['github.sourceId']}") String sourceId) {
        return new GitHubStep1Processor(sourceId);
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_WRITER)
    @StepScope
    public GitHubStep1Writer step1Writer() {
        return new GitHubStep1Writer(contestInternalApi);
    }

    private Integer parseIntParameter(String param, Integer defaultValue) {
        if (param != null && !param.isBlank()) {
            try {
                return Integer.parseInt(param);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse parameter: {}, using default: {}", param, defaultValue);
            }
        }
        return defaultValue;
    }
}
