package com.tech.n.ai.batch.source.domain.sources.sync.writer;

import com.tech.n.ai.datasource.mongodb.document.SourcesDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class SourcesRedisWriter implements ItemWriter<SourcesDocument> {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void write(Chunk<? extends SourcesDocument> chunk) {
        var items = chunk.getItems();
        
        if (items.isEmpty()) {
            return;
        }

        for (SourcesDocument document : items) {
            String sourceId = document.getId().toString();
            String key = document.getUrl() + ":" + document.getCategory();
            
            redisTemplate.opsForValue().set(key, sourceId);
        }
        
        log.info("Cached {} sources to Redis", items.size());
    }
}
