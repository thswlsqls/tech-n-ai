package com.tech.n.ai.batch.source.domain.contest.github.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.github.incrementer.ContestGitHubIncrementer;
import com.tech.n.ai.batch.source.domain.contest.github.jobparameter.ContestGitHubJobParameter;
import com.tech.n.ai.batch.source.domain.contest.github.processor.GitHubStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.github.reader.GitHubApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.github.service.GitHubApiService;
import com.tech.n.ai.batch.source.domain.contest.github.writer.GitHubStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto.Event;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
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
public class ContestGitHubApiJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${perPage:#{null}}")
    private String perPage;

    @Value("${page:#{null}}")
    private String page;

    private final GitHubApiService service;
    private final ContestGitHubJobParameter parameter;
    private final ContestInternalContract contestInternalApi;

    @Bean(name=Constants.CONTEST_GITHUB + Constants.PARAMETER)
    @JobScope
    public ContestGitHubJobParameter parameter() { 
        return new ContestGitHubJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_GITHUB)
    public Job ContestGitHubJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_GITHUB + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_GITHUB, jobRepository)
            .start(step1)
            .incrementer(new ContestGitHubIncrementer(baseDate, perPage, page))
            .build();
    }

    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_READER) GitHubApiPagingItemReader<Event> reader,
                      @Qualifier(Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_PROCESSOR) GitHubStep1Processor processor,
                      @Qualifier(Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_WRITER) GitHubStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_GITHUB + Constants.STEP_1, jobRepository)
            .<Event, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public GitHubApiPagingItemReader<Event> step1Reader(
            @Value("#{jobParameters['perPage']}") String perPage,
            @Value("#{jobParameters['page']}") String page) {
        Integer perPageValue = (perPage != null && !perPage.isBlank()) 
            ? Integer.parseInt(perPage) 
            : null;
        Integer pageValue = (page != null && !page.isBlank()) 
            ? Integer.parseInt(page) 
            : null;
        
        return new GitHubApiPagingItemReader<Event>(
            Constants.CHUNK_SIZE_10, 
            service,
            perPageValue,
            pageValue);
    }

    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public GitHubStep1Processor step1Processor() {
        return new GitHubStep1Processor();
    }

    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public GitHubStep1Writer step1Writer() {
        return new GitHubStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_GITHUB + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
