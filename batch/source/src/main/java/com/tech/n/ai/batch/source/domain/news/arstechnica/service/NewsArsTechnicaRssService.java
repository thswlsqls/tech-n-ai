package com.tech.n.ai.batch.source.domain.news.arstechnica.service;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.parser.ArsTechnicaRssParser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArsTechnicaRssService {

    private final ArsTechnicaRssParser rssParser;

    public List<RssFeedItem> getFeedItems() {
        return rssParser.parse();
    }
}
