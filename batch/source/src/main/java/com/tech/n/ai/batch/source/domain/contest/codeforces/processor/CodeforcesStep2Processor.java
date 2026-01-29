package com.tech.n.ai.batch.source.domain.contest.codeforces.processor;


import com.tech.n.ai.domain.mongodb.document.ContestDocument;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;


@Slf4j
@StepScope
public class CodeforcesStep2Processor implements ItemProcessor<ContestDocument, ContestDocument> {

    @Override
    public @Nullable ContestDocument process(ContestDocument item) throws Exception {
        if (!isFinishedContest(item)) {
            return null;
        }
        
        log.debug("Processing finished contest: {} (id: {})", item.getTitle(), item.getId());
        return item;
    }

    private boolean isFinishedContest(ContestDocument item) {
        if (item == null || item.getEndDate() == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = item.getEndDate().toLocalDate();
        
        return endDate.isBefore(today);
    }
}
