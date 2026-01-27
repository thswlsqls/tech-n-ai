package com.tech.n.ai.batch.source.domain.contest.codeforces.reader;

import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.batch.item.data.MongoPagingItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
public class CodeforcesStep2Reader extends MongoPagingItemReader<ContestDocument> {

    public CodeforcesStep2Reader(MongoTemplate mongoTemplate, String sourceId, int pageSize) {
        setTemplate(mongoTemplate);
        setTargetType(ContestDocument.class);
        setQuery(buildQuery(sourceId));
        setSort(buildSort());
        setPageSize(pageSize);
        setName("codeforcesStep2Reader");
    }

    private Query buildQuery(String sourceId) {
        ObjectId objectId = new ObjectId(sourceId);
        return new Query(Criteria.where("source_id").is(objectId));
    }

    private Map<String, Sort.Direction> buildSort() {
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("start_date", Sort.Direction.DESC);
        return sorts;
    }
}
