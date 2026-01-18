package com.tech.n.ai.batch.source.domain.contest.reddit.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.reddit.incrementer.ContestRedditIncrementer;
import com.tech.n.ai.batch.source.domain.contest.reddit.jobparameter.ContestRedditJobParameter;
import com.tech.n.ai.batch.source.domain.contest.reddit.processor.RedditStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.reddit.reader.RedditApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.reddit.service.RedditApiService;
import com.tech.n.ai.batch.source.domain.contest.reddit.writer.RedditStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.feign.domain.reddit.contract.RedditDto.Post;
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
public class ContestRedditApiJobConfig {

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

    private final RedditApiService service;
    private final ContestRedditJobParameter parameter;
    private final ContestInternalContract contestInternalApi;

    @Bean(name=Constants.CONTEST_REDDIT + Constants.PARAMETER)
    @JobScope
    public ContestRedditJobParameter parameter() { 
        return new ContestRedditJobParameter(); 
    }

    @Bean(name=Constants.CONTEST_REDDIT)
    public Job ContestRedditJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_REDDIT + Constants.STEP_1) Step step1) {

        return new JobBuilder(Constants.CONTEST_REDDIT, jobRepository)
            .start(step1)
            .incrementer(new ContestRedditIncrementer(baseDate, subreddit, limit, after, before))
            .build();
    }

    @Bean(name = Constants.CONTEST_REDDIT + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier(Constants.CONTEST_REDDIT + Constants.STEP_1 + Constants.ITEM_READER) RedditApiPagingItemReader<Post> reader,
                      @Qualifier(Constants.CONTEST_REDDIT + Constants.STEP_1 + Constants.ITEM_PROCESSOR) RedditStep1Processor processor,
                      @Qualifier(Constants.CONTEST_REDDIT + Constants.STEP_1 + Constants.ITEM_WRITER) RedditStep1Writer writer) {

        return new StepBuilder(Constants.CONTEST_REDDIT + Constants.STEP_1, jobRepository)
            .<Post, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.CONTEST_REDDIT + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public RedditApiPagingItemReader<Post> step1Reader(
            @Value("#{jobParameters['subreddit']}") String subreddit,
            @Value("#{jobParameters['limit']}") String limit,
            @Value("#{jobParameters['after']}") String after,
            @Value("#{jobParameters['before']}") String before) {
        Integer limitValue = (limit != null && !limit.isBlank()) 
            ? Integer.parseInt(limit) 
            : null;
        
        return new RedditApiPagingItemReader<Post>(
            Constants.CHUNK_SIZE_10, 
            service,
            subreddit,
            limitValue,
            after,
            before);
    }

    @Bean(name = Constants.CONTEST_REDDIT + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public RedditStep1Processor step1Processor() {
        return new RedditStep1Processor();
    }

    @Bean(name = Constants.CONTEST_REDDIT + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public RedditStep1Writer step1Writer() {
        return new RedditStep1Writer(contestInternalApi);
    }

    @Bean(name = Constants.CONTEST_REDDIT + Constants.BACKOFF_POLICY)
    public BackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000 * 60 * 2); // 기본 2분
        backOffPolicy.setMultiplier(1.2); // 1.2배씩 증가
        backOffPolicy.setMaxInterval(1000 * 60 * 5); // 최대 5분
        return backOffPolicy;
    }
}
