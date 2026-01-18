package com.tech.n.ai.api.contest.service;

import com.tech.n.ai.api.contest.common.exception.ContestDuplicateException;
import com.tech.n.ai.api.contest.common.exception.ContestNotFoundException;
import com.tech.n.ai.api.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import com.tech.n.ai.datasource.mongodb.repository.ContestRepository;
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
 * Contest Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestServiceImpl implements ContestService {
    
    private final ContestRepository contestRepository;
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Page<ContestDocument> findContests(ObjectId sourceId, String status, Pageable pageable) {
        Query query = new Query();
        
        if (sourceId != null) {
            query.addCriteria(Criteria.where("sourceId").is(sourceId));
        }
        
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        query.with(pageable);
        
        long total = mongoTemplate.count(query, ContestDocument.class);
        var list = mongoTemplate.find(query, ContestDocument.class);
        
        return new PageImpl<>(list, pageable, total);
    }
    
    @Override
    public ContestDocument findContestById(String id) {
        ObjectId objectId = new ObjectId(id);
        return contestRepository.findById(objectId)
            .orElseThrow(() -> new ContestNotFoundException("대회를 찾을 수 없습니다: " + id));
    }
    
    @Override
    public Page<ContestDocument> searchContest(String query, Pageable pageable) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage()
            .matching(query);
        
        Query mongoQuery = TextQuery.queryText(criteria)
            .with(pageable);
        
        long total = mongoTemplate.count(mongoQuery, ContestDocument.class);
        var list = mongoTemplate.find(mongoQuery, ContestDocument.class);
        
        return new PageImpl<>(list, pageable, total);
    }
    
    @Transactional
    @Override
    public ContestDocument saveContest(ContestCreateRequest request) {
        // 중복 체크
        if (contestRepository.existsBySourceIdAndUrl(
                new ObjectId(request.sourceId()),
                request.url())) {
            throw new ContestDuplicateException("이미 존재하는 대회입니다.");
        }
        
        // Document 생성 및 저장
        ContestDocument document = new ContestDocument();
        document.setSourceId(new ObjectId(request.sourceId()));
        document.setTitle(request.title());
        document.setStartDate(request.startDate());
        document.setEndDate(request.endDate());
        document.setStatus(calculateStatus(request.startDate(), request.endDate()));
        document.setDescription(request.description());
        document.setUrl(request.url());
        
        // Metadata 설정
        if (request.metadata() != null) {
            ContestDocument.ContestMetadata metadata = new ContestDocument.ContestMetadata();
            metadata.setSourceName(request.metadata().sourceName());
            metadata.setPrize(request.metadata().prize());
            metadata.setParticipants(request.metadata().participants());
            metadata.setTags(request.metadata().tags());
            document.setMetadata(metadata);
        }
        
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        return contestRepository.save(document);
    }
    
    /**
     * 대회 상태 계산
     * 
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 상태 (UPCOMING, ONGOING, ENDED)
     */
    private String calculateStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) {
            return "UPCOMING";
        } else if (now.isAfter(endDate)) {
            return "ENDED";
        } else {
            return "ONGOING";
        }
    }
}
