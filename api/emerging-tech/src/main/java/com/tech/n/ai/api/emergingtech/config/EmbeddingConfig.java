package com.tech.n.ai.api.emergingtech.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 임베딩 모델 설정 (OpenAI text-embedding-3-small)
 *
 * EmergingTech 문서 저장 시 벡터 임베딩을 자동 생성하기 위한 설정입니다.
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${langchain4j.open-ai.embedding-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.embedding-model.model-name:text-embedding-3-small}")
    private String modelName;

    @Value("${langchain4j.open-ai.embedding-model.dimensions:1536}")
    private Integer dimensions;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
            .apiKey(apiKey)
            .modelName(modelName)
            .dimensions(dimensions)
            .timeout(Duration.ofSeconds(30))
            .logRequests(true)
            .logResponses(false)
            .build();
    }
}
