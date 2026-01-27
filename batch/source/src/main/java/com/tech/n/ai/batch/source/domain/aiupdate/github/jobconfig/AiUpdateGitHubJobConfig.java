package com.tech.n.ai.batch.source.domain.aiupdate.github.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.aiupdate.dto.request.AiUpdateCreateRequest;
import com.tech.n.ai.batch.source.domain.aiupdate.github.incrementer.AiUpdateGitHubIncrementer;
import com.tech.n.ai.batch.source.domain.aiupdate.github.jobparameter.AiUpdateGitHubJobParameter;
import com.tech.n.ai.batch.source.domain.aiupdate.github.listener.AiUpdateGitHubJobListener;
import com.tech.n.ai.batch.source.domain.aiupdate.github.processor.GitHubReleasesProcessor;
import com.tech.n.ai.batch.source.domain.aiupdate.github.reader.GitHubReleaseWithRepo;
import com.tech.n.ai.batch.source.domain.aiupdate.github.reader.GitHubReleasesPagingItemReader;
import com.tech.n.ai.batch.source.domain.aiupdate.github.service.GitHubReleasesService;
import com.tech.n.ai.batch.source.domain.aiupdate.github.writer.GitHubReleasesWriter;
import com.tech.n.ai.client.feign.domain.internal.contract.AiUpdateInternalContract;
import com.tech.n.ai.datasource.mongodb.enums.AiProvider;
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
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * AI Update GitHub Releases Job 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiUpdateGitHubJobConfig {

    private static final String JOB_NAME = Constants.AI_UPDATE_GITHUB;
    private static final String STEP1_NAME = JOB_NAME + Constants.STEP_1;
    private static final int CHUNK_SIZE = Constants.CHUNK_SIZE_10;

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final GitHubReleasesService gitHubReleasesService;
    private final AiUpdateInternalContract aiUpdateInternalApi;

    // 대상 저장소 목록
    private static final List<GitHubReleasesPagingItemReader.RepositoryInfo> TARGET_REPOSITORIES = List.of(
        new GitHubReleasesPagingItemReader.RepositoryInfo("openai", "openai-python", AiProvider.OPENAI.name()),
        new GitHubReleasesPagingItemReader.RepositoryInfo("anthropics", "anthropic-sdk-python", AiProvider.ANTHROPIC.name()),
        new GitHubReleasesPagingItemReader.RepositoryInfo("google", "generative-ai-python", AiProvider.GOOGLE.name()),
        new GitHubReleasesPagingItemReader.RepositoryInfo("facebookresearch", "llama", AiProvider.META.name())
    );

    @Bean(name = JOB_NAME + Constants.PARAMETER)
    @JobScope
    public AiUpdateGitHubJobParameter parameter() {
        return new AiUpdateGitHubJobParameter();
    }

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository,
                   @Qualifier(STEP1_NAME) Step step1,
                   @Qualifier(JOB_NAME + ".listener") AiUpdateGitHubJobListener listener) {
        return new JobBuilder(JOB_NAME, jobRepository)
            .start(step1)
            .incrementer(new AiUpdateGitHubIncrementer(baseDate))
            .listener(listener)
            .build();
    }

    @Bean(name = JOB_NAME + ".listener")
    public AiUpdateGitHubJobListener jobListener() {
        return new AiUpdateGitHubJobListener();
    }

    @Bean(name = STEP1_NAME)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(STEP1_NAME + Constants.ITEM_READER) GitHubReleasesPagingItemReader reader,
                      @Qualifier(STEP1_NAME + Constants.ITEM_PROCESSOR) GitHubReleasesProcessor processor,
                      @Qualifier(STEP1_NAME + Constants.ITEM_WRITER) GitHubReleasesWriter writer) {
        return new StepBuilder(STEP1_NAME, jobRepository)
            .<GitHubReleaseWithRepo, AiUpdateCreateRequest>chunk(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_READER)
    @StepScope
    public GitHubReleasesPagingItemReader step1Reader() {
        return new GitHubReleasesPagingItemReader(CHUNK_SIZE, gitHubReleasesService, TARGET_REPOSITORIES);
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_PROCESSOR)
    @StepScope
    public GitHubReleasesProcessor step1Processor() {
        return new GitHubReleasesProcessor();
    }

    @Bean(name = STEP1_NAME + Constants.ITEM_WRITER)
    @StepScope
    public GitHubReleasesWriter step1Writer() {
        return new GitHubReleasesWriter(aiUpdateInternalApi);
    }
}
