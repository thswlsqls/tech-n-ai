package com.tech.n.ai.api.agent.service;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.parser.GoogleAiBlogRssParser;
import com.tech.n.ai.client.rss.parser.OpenAiBlogRssParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent용 RSS 피드 수집 서비스
 * RSS 파서를 통해 피드를 수집하고 제공자별 필터링 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssDataCollectionService {

    private final OpenAiBlogRssParser openAiBlogRssParser;
    private final GoogleAiBlogRssParser googleAiBlogRssParser;

    /**
     * RSS 피드 수집
     *
     * @param provider 제공자 필터 ("OPENAI", "GOOGLE", 빈 문자열=전체)
     * @return 수집된 RSS 피드 아이템 목록
     */
    public List<RssFeedItem> fetchRssFeeds(String provider) {
        List<RssFeedItem> allItems = new ArrayList<>();

        boolean fetchAll = provider == null || provider.isBlank();

        if (fetchAll || "OPENAI".equalsIgnoreCase(provider)) {
            allItems.addAll(parseOpenAi());
        }

        if (fetchAll || "GOOGLE".equalsIgnoreCase(provider)) {
            allItems.addAll(parseGoogle());
        }

        log.info("RSS 피드 수집 완료: provider={}, total={}", provider, allItems.size());
        return allItems;
    }

    private List<RssFeedItem> parseOpenAi() {
        try {
            List<RssFeedItem> items = openAiBlogRssParser.parse();
            log.info("OpenAI RSS 파싱 완료: {} items", items.size());
            return items;
        } catch (Exception e) {
            log.error("OpenAI RSS 파싱 실패", e);
            return List.of();
        }
    }

    private List<RssFeedItem> parseGoogle() {
        try {
            List<RssFeedItem> items = googleAiBlogRssParser.parse();
            log.info("Google AI RSS 파싱 완료: {} items", items.size());
            return items;
        } catch (Exception e) {
            log.error("Google AI RSS 파싱 실패", e);
            return List.of();
        }
    }
}
