package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.api.archive.dto.request.ArchiveListRequest;
import com.tech.n.ai.api.archive.dto.request.ArchiveSearchRequest;
import com.tech.n.ai.datasource.mongodb.document.ArchiveDocument;
import com.tech.n.ai.datasource.mongodb.repository.ArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Archive Query Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveQueryServiceImpl implements ArchiveQueryService {
    
    private final ArchiveRepository archiveRepository;
    private final MongoTemplate mongoTemplate;
    
    @Override
    public Page<ArchiveDocument> findArchives(String userId, ArchiveListRequest request) {
        // 페이징 및 정렬 처리
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            parseSort(request.sort())
        );
        
        // userId 필터링 및 itemType 필터링
        if (request.itemType() != null && !request.itemType().isBlank()) {
            return archiveRepository.findByUserIdAndItemType(userId, request.itemType(), pageable);
        } else {
            return archiveRepository.findByUserId(userId, pageable);
        }
    }
    
    @Override
    public ArchiveDocument findArchiveById(String userId, String id) {
        Optional<ArchiveDocument> documentOpt;
        
        // archiveTsid 또는 ObjectId로 조회 시도
        try {
            // 먼저 archiveTsid로 조회 시도
            documentOpt = archiveRepository.findByArchiveTsid(id);
            
            // archiveTsid로 찾지 못한 경우 ObjectId로 조회 시도
            if (documentOpt.isEmpty()) {
                ObjectId objectId = new ObjectId(id);
                documentOpt = archiveRepository.findById(objectId);
            }
        } catch (IllegalArgumentException e) {
            // ObjectId 파싱 실패 시 archiveTsid로만 조회
            documentOpt = archiveRepository.findByArchiveTsid(id);
        }
        
        ArchiveDocument document = documentOpt
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + id));
        
        // 권한 검증 (본인의 아카이브만 조회 가능)
        if (!document.getUserId().equals(userId)) {
            throw new UnauthorizedException("본인의 아카이브만 조회할 수 있습니다.");
        }
        
        return document;
    }
    
    @Override
    public Page<ArchiveDocument> searchArchives(String userId, ArchiveSearchRequest request) {
        // 페이징 처리
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size()
        );
        
        // 검색 필드에 따라 쿼리 구성
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        
        String searchTerm = request.q();
        String searchField = request.searchField();
        
        if ("tag".equals(searchField)) {
            // tag 필드만 검색
            query.addCriteria(Criteria.where("tag").regex(searchTerm, "i"));
        } else if ("memo".equals(searchField)) {
            // memo 필드만 검색
            query.addCriteria(Criteria.where("memo").regex(searchTerm, "i"));
        } else {
            // all: tag 또는 memo 필드에서 검색
            query.addCriteria(new Criteria().orOperator(
                Criteria.where("tag").regex(searchTerm, "i"),
                Criteria.where("memo").regex(searchTerm, "i")
            ));
        }
        
        query.with(pageable);
        
        long total = mongoTemplate.count(query, ArchiveDocument.class);
        var list = mongoTemplate.find(query, ArchiveDocument.class);
        
        return new org.springframework.data.domain.PageImpl<>(list, pageable, total);
    }
    
    /**
     * 정렬 문자열을 Sort 객체로 변환
     * 
     * @param sort 정렬 문자열 (예: "archivedAt,desc")
     * @return Sort 객체
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "archivedAt");
        }
        
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "archivedAt");
        }
        
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();
        
        // MongoDB 필드명으로 변환 (camelCase -> snake_case)
        String mongoField = convertToMongoField(field);
        
        Sort.Direction sortDirection = "asc".equals(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(sortDirection, mongoField);
    }
    
    /**
     * Java 필드명을 MongoDB 필드명으로 변환
     * 
     * @param field Java 필드명 (camelCase)
     * @return MongoDB 필드명 (snake_case)
     */
    private String convertToMongoField(String field) {
        // 지원하는 정렬 필드 매핑
        return switch (field) {
            case "archivedAt" -> "archived_at";
            case "createdAt" -> "created_at";
            case "itemTitle" -> "item_title";
            case "itemStartDate" -> "item_start_date";
            case "itemEndDate" -> "item_end_date";
            case "itemPublishedAt" -> "item_published_at";
            default -> field; // 기본값은 그대로 사용
        };
    }
}
