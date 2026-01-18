package com.tech.n.ai.api.news.service;

import com.tech.n.ai.api.news.common.exception.NewsDuplicateException;
import com.tech.n.ai.api.news.common.exception.NewsNotFoundException;
import com.tech.n.ai.api.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.datasource.mongodb.document.NewsArticleDocument;
import com.tech.n.ai.datasource.mongodb.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * News Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {
    
    private final NewsArticleRepository newsArticleRepository;
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Page<NewsArticleDocument> findNewsArticles(ObjectId sourceId, Pageable pageable) {
        Query query = new Query();
        
        if (sourceId != null) {
            query.addCriteria(Criteria.where("sourceId").is(sourceId));
        }
        
        query.with(pageable);
        
        long total = mongoTemplate.count(query, NewsArticleDocument.class);
        var list = mongoTemplate.find(query, NewsArticleDocument.class);
        
        return new PageImpl<>(list, pageable, total);
    }
    
    @Override
    public NewsArticleDocument findNewsArticleById(String id) {
        ObjectId objectId = new ObjectId(id);
        return newsArticleRepository.findById(objectId)
            .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + id));
    }
    
    @Override
    public Page<NewsArticleDocument> searchNewsArticles(String query, Pageable pageable) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage()
            .matching(query);
        
        Query mongoQuery = TextQuery.queryText(criteria)
            .with(pageable);
        
        long total = mongoTemplate.count(mongoQuery, NewsArticleDocument.class);
        var list = mongoTemplate.find(mongoQuery, NewsArticleDocument.class);
        
        return new PageImpl<>(list, pageable, total);
    }
    
    @Transactional
    @Override
    public NewsArticleDocument saveNews(NewsCreateRequest request) {
        // 중복 체크
        if (newsArticleRepository.existsBySourceIdAndUrl(
                new ObjectId(request.sourceId()),
                request.url())) {
            throw new NewsDuplicateException("이미 존재하는 뉴스 기사입니다.");
        }
        
        // Document 생성 및 저장
        NewsArticleDocument document = new NewsArticleDocument();
        document.setSourceId(new ObjectId(request.sourceId()));
        document.setTitle(request.title());
        document.setContent(request.content());
        document.setSummary(request.summary());
        document.setPublishedAt(request.publishedAt());
        document.setUrl(request.url());
        document.setAuthor(request.author());
        
        // Metadata 설정
        if (request.metadata() != null) {
            NewsArticleDocument.NewsArticleMetadata metadata = new NewsArticleDocument.NewsArticleMetadata();
            metadata.setSourceName(request.metadata().sourceName());
            metadata.setTags(request.metadata().tags());
            metadata.setViewCount(request.metadata().viewCount());
            metadata.setLikeCount(request.metadata().likeCount());
            document.setMetadata(metadata);
        }
        
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        return newsArticleRepository.save(document);
    }
}
