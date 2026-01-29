package com.tech.n.ai.client.scraper.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 기사 상세 페이지에서 publishedAt을 추출하는 Fallback 유틸리티.
 *
 * 추출 우선순위:
 * 1. OG meta: article:published_time
 * 2. JSON-LD: datePublished
 * 3. HTML5 time[datetime]
 * 4. __next_f 스크립트: publishedOn / _createdAt
 * 5. 본문 텍스트 정규식 매칭
 */
@Component
@Slf4j
public class StructuredDataDateExtractor {

    private final WebClient.Builder webClientBuilder;

    private static final Pattern DATE_TEXT_PATTERN = Pattern.compile(
            "(?:January|February|March|April|May|June|July|August|September|October|November|December" +
            "|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},\\s+\\d{4}");

    private static final Pattern JSON_LD_DATE_PATTERN = Pattern.compile(
            "\"datePublished\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern NEXT_PUBLISHED_ON_PATTERN = Pattern.compile(
            "\"publishedOn\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern NEXT_CREATED_AT_PATTERN = Pattern.compile(
            "\"_createdAt\"\\s*:\\s*\"([^\"]+)\"");

    public StructuredDataDateExtractor(
            @Qualifier("scraperWebClientBuilder") WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * 기사 상세 페이지를 fetch하여 publishedAt을 추출.
     * null이면 모든 전략이 실패한 것.
     */
    public LocalDateTime extractFromArticlePage(String articleUrl) {
        if (articleUrl == null || articleUrl.isBlank()) {
            return null;
        }

        try {
            log.debug("Fetching article page for date extraction: {}", articleUrl);

            String html = webClientBuilder.build().get()
                    .uri(articleUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (html == null || html.isEmpty()) {
                return null;
            }

            Document doc = Jsoup.parse(html);

            // 1순위: OG meta tag
            LocalDateTime result = extractFromOgMeta(doc);
            if (result != null) {
                log.debug("Extracted date from OG meta: {}", result);
                return result;
            }

            // 2순위: JSON-LD
            result = extractFromJsonLd(doc);
            if (result != null) {
                log.debug("Extracted date from JSON-LD: {}", result);
                return result;
            }

            // 3순위: HTML5 time[datetime]
            result = extractFromTimeElement(doc);
            if (result != null) {
                log.debug("Extracted date from <time> element: {}", result);
                return result;
            }

            // 4순위: __next_f 스크립트 (publishedOn / _createdAt)
            result = extractFromNextData(doc);
            if (result != null) {
                log.debug("Extracted date from __next_f script: {}", result);
                return result;
            }

            // 5순위: 본문 텍스트 정규식 매칭
            result = extractFromBodyText(doc);
            if (result != null) {
                log.debug("Extracted date from body text regex: {}", result);
                return result;
            }

            log.debug("All date extraction strategies failed for: {}", articleUrl);
            return null;

        } catch (Exception e) {
            log.warn("Failed to fetch article page for date extraction: {} - {}", articleUrl, e.getMessage());
            return null;
        }
    }

    private LocalDateTime extractFromOgMeta(Document doc) {
        Element meta = doc.selectFirst("meta[property=article:published_time]");
        if (meta != null) {
            return DateParsingUtils.parseDateText(meta.attr("content"));
        }
        // og:published_time 변형
        meta = doc.selectFirst("meta[property=og:published_time]");
        if (meta != null) {
            return DateParsingUtils.parseDateText(meta.attr("content"));
        }
        // article:published
        meta = doc.selectFirst("meta[property=article:published]");
        if (meta != null) {
            return DateParsingUtils.parseDateText(meta.attr("content"));
        }
        return null;
    }

    private LocalDateTime extractFromJsonLd(Document doc) {
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            String content = script.data();
            Matcher matcher = JSON_LD_DATE_PATTERN.matcher(content);
            if (matcher.find()) {
                LocalDateTime result = DateParsingUtils.parseDateText(matcher.group(1));
                if (result != null) return result;
            }
        }
        return null;
    }

    private LocalDateTime extractFromTimeElement(Document doc) {
        Element time = doc.selectFirst("time[datetime]");
        if (time != null) {
            return DateParsingUtils.parseDateText(time.attr("datetime"));
        }
        return null;
    }

    private LocalDateTime extractFromNextData(Document doc) {
        Elements scripts = doc.select("script");
        for (Element script : scripts) {
            String content = script.data();

            // __next_f 이중 이스케이프 해제
            String unescaped = content
                    .replace("\\\"", "\"")
                    .replace("\\/", "/")
                    .replace("\\_", "_");

            // publishedOn 우선
            Matcher matcher = NEXT_PUBLISHED_ON_PATTERN.matcher(unescaped);
            if (matcher.find()) {
                LocalDateTime result = DateParsingUtils.parseDateText(matcher.group(1));
                if (result != null) return result;
            }

            // _createdAt fallback
            matcher = NEXT_CREATED_AT_PATTERN.matcher(unescaped);
            if (matcher.find()) {
                LocalDateTime result = DateParsingUtils.parseDateText(matcher.group(1));
                if (result != null) return result;
            }
        }
        return null;
    }

    private LocalDateTime extractFromBodyText(Document doc) {
        String bodyText = doc.body() != null ? doc.body().text() : "";
        Matcher matcher = DATE_TEXT_PATTERN.matcher(bodyText);
        if (matcher.find()) {
            return DateParsingUtils.parseDateText(matcher.group());
        }
        return null;
    }
}
