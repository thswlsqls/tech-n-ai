package com.tech.n.ai.batch.source.domain.sources.sync.jobconfig;


import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.sources.sync.dto.SourceJsonDto;
import com.tech.n.ai.batch.source.domain.sources.sync.incrementer.SourcesSyncIncrementer;
import com.tech.n.ai.batch.source.domain.sources.sync.processor.SourcesSyncProcessor;
import com.tech.n.ai.batch.source.domain.sources.sync.reader.SourcesJsonItemReader;
import com.tech.n.ai.batch.source.domain.sources.sync.writer.SourcesMongoWriter;
import com.tech.n.ai.batch.source.domain.sources.sync.writer.SourcesRedisWriter;
import com.tech.n.ai.domain.mongodb.document.SourcesDocument;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.MongoPagingItemReader;
import org.springframework.batch.item.data.builder.MongoPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class SourcesSyncJobConfig {

    @Value("${baseDate:#{null}}")
    private String baseDate;

    private final MongoTemplate mongoTemplate;

    @Bean(name = Constants.SOURCES_SYNC)
    public Job sourcesSyncJob(JobRepository jobRepository,
                              @Qualifier(Constants.SOURCES_SYNC + Constants.STEP_1) Step step1,
                              @Qualifier(Constants.SOURCES_SYNC + Constants.STEP_2) Step step2) {
        return new JobBuilder(Constants.SOURCES_SYNC, jobRepository)
            .start(step1)
            .next(step2)
            .incrementer(new SourcesSyncIncrementer(baseDate))
            .build();
    }

    @Bean(name = Constants.SOURCES_SYNC + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(Constants.SOURCES_SYNC + Constants.STEP_1 + Constants.ITEM_READER) SourcesJsonItemReader reader,
                      @Qualifier(Constants.SOURCES_SYNC + Constants.STEP_1 + Constants.ITEM_PROCESSOR) SourcesSyncProcessor processor,
                      @Qualifier(Constants.SOURCES_SYNC + Constants.STEP_1 + Constants.ITEM_WRITER) SourcesMongoWriter writer) {
        return new StepBuilder(Constants.SOURCES_SYNC + Constants.STEP_1, jobRepository)
            .<SourceJsonDto, SourcesDocument>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.SOURCES_SYNC + Constants.STEP_1 + Constants.ITEM_READER)
    @StepScope
    public SourcesJsonItemReader step1Reader() {
        return new SourcesJsonItemReader(Constants.CHUNK_SIZE_10);
    }

    @Bean(name = Constants.SOURCES_SYNC + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    @StepScope
    public SourcesSyncProcessor step1Processor() {
        return new SourcesSyncProcessor();
    }

    @Bean(name = Constants.SOURCES_SYNC + Constants.STEP_1 + Constants.ITEM_WRITER)
    @StepScope
    public SourcesMongoWriter step1Writer() {
        return new SourcesMongoWriter(mongoTemplate);
    }

    @Bean(name = Constants.SOURCES_SYNC + Constants.STEP_2)
    @JobScope
    public Step step2(JobRepository jobRepository,
                      @Qualifier("primaryPlatformTransactionManager") PlatformTransactionManager transactionManager,
                      @Qualifier(Constants.SOURCES_SYNC + Constants.STEP_2 + Constants.ITEM_READER) MongoPagingItemReader<SourcesDocument> reader,
                      @Qualifier(Constants.SOURCES_SYNC + Constants.STEP_2 + Constants.ITEM_WRITER) SourcesRedisWriter writer) {
        return new StepBuilder(Constants.SOURCES_SYNC + Constants.STEP_2, jobRepository)
            .<SourcesDocument, SourcesDocument>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .writer(writer)
            .build();
    }

    @Bean(name = Constants.SOURCES_SYNC + Constants.STEP_2 + Constants.ITEM_READER)
    @StepScope
    public MongoPagingItemReader<SourcesDocument> step2Reader() {
        return new MongoPagingItemReaderBuilder<SourcesDocument>()
            .name(Constants.SOURCES_SYNC + Constants.STEP_2 + Constants.ITEM_READER)
            .template(mongoTemplate)
            .targetType(SourcesDocument.class)
            .jsonQuery("{}")
            .sorts(Map.of("_id", Sort.Direction.ASC))
            .pageSize(Constants.CHUNK_SIZE_10)
            .collection("sources")
            .build();
    }

    @Bean(name = Constants.SOURCES_SYNC + Constants.STEP_2 + Constants.ITEM_WRITER)
    @StepScope
    public SourcesRedisWriter step2Writer(RedisTemplate<String, String> redisTemplate) {
        return new SourcesRedisWriter(redisTemplate);
    }
}
