package com.tech.n.ai.batch.source.domain.contest.devpost.service;

import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import com.tech.n.ai.client.scraper.scraper.DevpostScraper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestDevpostScraperService {

    private final DevpostScraper scraper;

    public List<ScrapedContestItem> getScrapedItems() {
        return scraper.scrape();
    }
}
