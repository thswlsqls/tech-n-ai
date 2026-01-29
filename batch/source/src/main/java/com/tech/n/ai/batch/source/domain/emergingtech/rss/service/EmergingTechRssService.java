package com.tech.n.ai.batch.source.domain.emergingtech.rss.service;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.parser.GoogleAiBlogRssParser;
import com.tech.n.ai.client.rss.parser.OpenAiBlogRssParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Emerging Tech RSS 수집 서비스
 * OpenAI + Google AI Blog RSS 피드를 모두 수집
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergingTechRssService {

    private final OpenAiBlogRssParser openAiParser;
    private final GoogleAiBlogRssParser googleAiParser;

    public List<RssFeedItem> fetchAllEmergingTechFeeds() {
        List<RssFeedItem> allItems = new ArrayList<>();

        try {
            List<RssFeedItem> openAiItems = openAiParser.parse();
            log.info("OpenAI Blog RSS: {} items fetched", openAiItems.size());
            allItems.addAll(openAiItems);
        } catch (Exception e) {
            log.error("Failed to fetch OpenAI Blog RSS", e);
        }

        try {
            List<RssFeedItem> googleItems = googleAiParser.parse();
            log.info("Google AI Blog RSS: {} items fetched", googleItems.size());
            allItems.addAll(googleItems);
        } catch (Exception e) {
            log.error("Failed to fetch Google AI Blog RSS", e);
        }

        return allItems;
    }
}
