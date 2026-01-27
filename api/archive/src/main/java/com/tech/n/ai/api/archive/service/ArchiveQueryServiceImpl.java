package com.tech.n.ai.api.archive.service;

import com.tech.n.ai.api.archive.common.exception.ArchiveNotFoundException;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.api.archive.dto.request.ArchiveListRequest;
import com.tech.n.ai.api.archive.dto.request.ArchiveSearchRequest;
import com.tech.n.ai.datasource.mariadb.entity.archive.ArchiveEntity;
import com.tech.n.ai.datasource.mariadb.repository.reader.archive.ArchiveReaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Archive Query Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveQueryServiceImpl implements ArchiveQueryService {
    
    private final ArchiveReaderRepository archiveReaderRepository;
    
    @Override
    public Page<ArchiveEntity> findArchives(Long userId, ArchiveListRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            parseSort(request.sort())
        );
        
        Specification<ArchiveEntity> spec = Specification.where(null);
        spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        spec = spec.and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));
        
        if (request.itemType() != null && !request.itemType().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("itemType"), request.itemType()));
        }
        
        return archiveReaderRepository.findAll(spec, pageable);
    }
    
    @Override
    public ArchiveEntity findArchiveById(Long userId, Long id) {
        ArchiveEntity entity = archiveReaderRepository.findById(id)
            .orElseThrow(() -> new ArchiveNotFoundException("아카이브를 찾을 수 없습니다: " + id));
        
        if (!entity.isOwnedBy(userId)) {
            throw new UnauthorizedException("본인의 아카이브만 조회할 수 있습니다.");
        }
        
        if (Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new ArchiveNotFoundException("삭제된 아카이브입니다: " + id);
        }
        
        return entity;
    }
    
    @Override
    public Page<ArchiveEntity> searchArchives(Long userId, ArchiveSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Specification<ArchiveEntity> spec = Specification.where(null);
        spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        spec = spec.and((root, query, cb) -> cb.equal(root.get("isDeleted"), false));
        
        String searchTerm = request.q();
        String searchField = request.searchField();
        
        if ("tag".equals(searchField)) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("tag")), "%" + searchTerm.toLowerCase() + "%"));
        } else if ("memo".equals(searchField)) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("memo")), "%" + searchTerm.toLowerCase() + "%"));
        } else {
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("tag")), "%" + searchTerm.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("memo")), "%" + searchTerm.toLowerCase() + "%")
            ));
        }
        
        return archiveReaderRepository.findAll(spec, pageable);
    }
    
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();
        
        Sort.Direction sortDirection = "asc".equals(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(sortDirection, field);
    }
}
