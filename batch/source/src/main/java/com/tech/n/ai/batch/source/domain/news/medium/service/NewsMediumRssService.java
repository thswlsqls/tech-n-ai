package com.tech.n.ai.batch.source.domain.news.medium.service;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.parser.MediumTechnologyRssParser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsMediumRssService {

    private final MediumTechnologyRssParser rssParser;

    public List<RssFeedItem> getFeedItems() {
        return rssParser.parse();
    }
}
