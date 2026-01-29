package com.tech.n.ai.batch.source.domain.contest.codeforces.writer;

import com.tech.n.ai.domain.mongodb.document.ContestDocument;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class CodeforcesStep2Writer implements ItemWriter<ContestDocument> {

    private final MongoTemplate mongoTemplate;

    @Override
    public void write(Chunk<? extends ContestDocument> chunk) throws Exception {
        List<? extends ContestDocument> items = chunk.getItems();
        
        if (items.isEmpty()) {
            log.warn("No items to delete");
            return;
        }
        
        List<ObjectId> idsToDelete = items.stream()
            .map(ContestDocument::getId)
            .collect(Collectors.toList());
        
        Query deleteQuery = new Query(Criteria.where("_id").in(idsToDelete));
        long deletedCount = mongoTemplate.remove(deleteQuery, ContestDocument.class).getDeletedCount();
        
        log.info("Deleted {} finished contests from MongoDB", deletedCount);
        items.forEach(item -> log.debug("Deleted contest: {} (id: {})", item.getTitle(), item.getId()));
    }
}
