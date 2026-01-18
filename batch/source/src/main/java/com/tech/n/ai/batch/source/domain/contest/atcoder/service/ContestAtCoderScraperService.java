package com.tech.n.ai.batch.source.domain.contest.atcoder.service;

import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import com.tech.n.ai.client.scraper.scraper.AtCoderScraper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestAtCoderScraperService {

    private final AtCoderScraper scraper;

    public List<ScrapedContestItem> getScrapedItems() {
        return scraper.scrape();
    }
}
