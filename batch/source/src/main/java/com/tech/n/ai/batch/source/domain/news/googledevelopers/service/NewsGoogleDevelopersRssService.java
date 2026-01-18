package com.tech.n.ai.batch.source.domain.news.googledevelopers.service;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.parser.GoogleDevelopersBlogRssParser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsGoogleDevelopersRssService {

    private final GoogleDevelopersBlogRssParser rssParser;

    public List<RssFeedItem> getFeedItems() {
        return rssParser.parse();
    }
}
