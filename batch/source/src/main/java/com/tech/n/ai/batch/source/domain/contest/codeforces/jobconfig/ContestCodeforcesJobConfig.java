package com.tech.n.ai.batch.source.domain.contest.codeforces.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.codeforces.incrementer.ContestCodeforceIncrementer;
import com.tech.n.ai.batch.source.domain.contest.codeforces.jobparameter.ContestCodeforcesJobParameter;
import com.tech.n.ai.batch.source.domain.contest.codeforces.listener.CodeforcesJobListener;
import com.tech.n.ai.batch.source.domain.contest.codeforces.processor.CodeforcesStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.codeforces.processor.CodeforcesStep2Processor;
import com.tech.n.ai.batch.source.domain.contest.codeforces.reader.CodeforcesApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.codeforces.reader.CodeforcesStep2Reader;
import com.tech.n.ai.batch.source.domain.contest.codeforces.service.CodeforcesApiService;
import com.tech.n.ai.batch.source.domain.contest.codeforces.writer.CodeforcesStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.codeforces.writer.CodeforcesStep2Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto.Contest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.domain.mongodb.document.ContestDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
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
public class ContestCodeforcesJobConfig {

    private static final String JOB_NAME = Constants.CONTEST_CODEFORCES;
    private static final String STEP1_NAME = JOB_NAME + Constants.STEP_1;
    private static final String STEP2_NAME = JOB_NAME + Constants.STEP_2;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_500;
    private static final boolean DEFAULT_GYM_VALUE = false;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    @Value("${gym:#{true}}")
    private String gym;

    private final CodeforcesApiService service;
    private final ContestInternalContract contestInternalApi;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean(name = JOB_NAME + Constants.PARAMETER)
    @JobScope
    public ContestCodeforcesJobParameter parameter() {
        return new ContestCodeforcesJobParameter();
    }

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository, 
                   @Qualifier(STEP1_NAME) Step step1,
                   @Qualifier(STEP2_NAME) Step step2,
                   @Qualifier(JOB_NAME + ".listener") CodeforcesJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step1)
            .next(step2)
            .incrementer(new ContestCodeforceIncrementer(baseDate, gym))
            .listener(listener)
            .build();
    }

    @Bean(name = JOB_NAME + ".listener")
    public CodeforcesJobListener jobListener() {
        return new CodeforcesJobListener(redisTemplate);
    }

    @Bean(name = STEP1_NAME)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP1_NAME + Constants.ITEM_READER) CodeforcesApiPagingItemReader reader,
                      @Qualifier(STEP1_NAME + Constants.ITEM_PROCESSOR) CodeforcesStep1Processor processor,
                      @Qualifier(STEP1_NAME + Constants.ITEM_WRITER) CodeforcesStep1Writer writer) {
        return new StepBuilder(STEP1_NAME, jobRepository)
            .<Contest, ContestCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_READER)
    @StepScope
    public CodeforcesApiPagingItemReader step1Reader(@Value("#{jobParameters['gym']}") String gymParam) {
        boolean includeGym = parseGymParameter(gymParam);
        return new CodeforcesApiPagingItemReader(CHUNK_SIZE, service, includeGym);
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public CodeforcesStep1Processor step1Processor(@Value("#{jobExecutionContext['codeforces.sourceId']}") String sourceId) {
        return new CodeforcesStep1Processor(sourceId);
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_WRITER)
    @StepScope
    public CodeforcesStep1Writer step1Writer() {
        return new CodeforcesStep1Writer(contestInternalApi);
    }

    @Bean(name = STEP2_NAME)
    @JobScope
    public Step step2(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP2_NAME + Constants.ITEM_READER) CodeforcesStep2Reader reader,
                      @Qualifier(STEP2_NAME + Constants.ITEM_PROCESSOR) CodeforcesStep2Processor processor,
                      @Qualifier(STEP2_NAME + Constants.ITEM_WRITER) CodeforcesStep2Writer writer) {
        return new StepBuilder(STEP2_NAME, jobRepository)
            .<ContestDocument, ContestDocument>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP2_NAME + Constants.ITEM_READER)
    @StepScope
    public CodeforcesStep2Reader step2Reader(@Value("#{jobExecutionContext['codeforces.sourceId']}") String sourceId) {
        return new CodeforcesStep2Reader(mongoTemplate, sourceId, CHUNK_SIZE);
    }

    @Bean(name = STEP2_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public CodeforcesStep2Processor step2Processor() {
        return new CodeforcesStep2Processor();
    }

    @Bean(name = STEP2_NAME + Constants.ITEM_WRITER)
    @StepScope
    public CodeforcesStep2Writer step2Writer() {
        return new CodeforcesStep2Writer(mongoTemplate);
    }

    private boolean parseGymParameter(String gymParam) {
        return gymParam != null ? Boolean.parseBoolean(gymParam) : DEFAULT_GYM_VALUE;
    }
}

