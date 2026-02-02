package com.tech.n.ai.api.emergingtech.service;

import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import com.tech.n.ai.domain.mongodb.repository.EmergingTechRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Emerging Tech Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergingTechServiceImpl implements EmergingTechService {

    private final EmergingTechRepository emergingTechRepository;

    @Override
    public Page<EmergingTechDocument> findEmergingTechs(String provider, String updateType, String status, Pageable pageable) {
        // 필터 조합에 따른 조회
        if (provider != null && status != null) {
            return emergingTechRepository.findByProviderAndStatus(provider, status, pageable);
        } else if (provider != null) {
            return emergingTechRepository.findByProvider(provider, pageable);
        } else if (status != null) {
            return emergingTechRepository.findByStatus(status, pageable);
        }
        return emergingTechRepository.findAll(pageable);
    }

    @Override
    public EmergingTechDocument findEmergingTechById(String id) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Emerging Tech를 찾을 수 없습니다: " + id);
        }

        return emergingTechRepository.findById(objectId)
            .orElseThrow(() -> new ResourceNotFoundException("Emerging Tech를 찾을 수 없습니다: " + id));
    }

    @Override
    public Page<EmergingTechDocument> searchEmergingTech(String query, Pageable pageable) {
        return emergingTechRepository.findByTitleContainingIgnoreCase(query, pageable);
    }

    @Override
    public EmergingTechDocument saveEmergingTech(EmergingTechCreateRequest request) {
        // 중복 체크 (externalId 기준) - 이미 존재하면 기존 데이터 반환
        if (request.externalId() != null) {
            var existing = emergingTechRepository.findByExternalId(request.externalId());
            if (existing.isPresent()) {
                log.debug("이미 존재하는 Emerging Tech: externalId={}", request.externalId());
                return existing.get();
            }
        }

        // 중복 체크 (url 기준) - 소스가 달라도 같은 URL이면 중복
        if (request.url() != null) {
            var existingByUrl = emergingTechRepository.findByUrl(request.url());
            if (existingByUrl.isPresent()) {
                log.debug("이미 존재하는 Emerging Tech: url={}", request.url());
                return existingByUrl.get();
            }
        }

        EmergingTechDocument document = new EmergingTechDocument();
        document.setProvider(request.provider());
        document.setUpdateType(request.updateType());
        document.setTitle(request.title());
        document.setSummary(request.summary());
        document.setUrl(request.url());
        document.setPublishedAt(request.publishedAt());
        document.setSourceType(request.sourceType());
        document.setStatus(request.status());
        document.setExternalId(request.externalId());

        // 메타데이터 설정
        if (request.metadata() != null) {
            EmergingTechDocument.EmergingTechMetadata metadata = new EmergingTechDocument.EmergingTechMetadata();
            metadata.setVersion(request.metadata().version());
            metadata.setTags(request.metadata().tags());
            metadata.setAuthor(request.metadata().author());
            metadata.setGithubRepo(request.metadata().githubRepo());
            metadata.setAdditionalInfo(request.metadata().additionalInfo());
            document.setMetadata(metadata);
        }

        // 타임스탬프 설정
        LocalDateTime now = LocalDateTime.now();
        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        return emergingTechRepository.save(document);
    }

    @Override
    public EmergingTechDocument updateStatus(String id, String status) {
        EmergingTechDocument document = findEmergingTechById(id);
        document.setStatus(status);
        document.setUpdatedAt(LocalDateTime.now());
        return emergingTechRepository.save(document);
    }
}
