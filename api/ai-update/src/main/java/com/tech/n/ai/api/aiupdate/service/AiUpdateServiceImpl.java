package com.tech.n.ai.api.aiupdate.service;

import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateCreateRequest;
import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import com.tech.n.ai.datasource.mongodb.document.AiUpdateDocument;
import com.tech.n.ai.datasource.mongodb.repository.AiUpdateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI Update Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUpdateServiceImpl implements AiUpdateService {

    private final AiUpdateRepository aiUpdateRepository;

    @Override
    public Page<AiUpdateDocument> findAiUpdates(String provider, String updateType, String status, Pageable pageable) {
        // 필터 조합에 따른 조회
        if (provider != null && status != null) {
            return aiUpdateRepository.findByProviderAndStatus(provider, status, pageable);
        } else if (provider != null) {
            return aiUpdateRepository.findByProvider(provider, pageable);
        } else if (status != null) {
            return aiUpdateRepository.findByStatus(status, pageable);
        }
        return aiUpdateRepository.findAll(pageable);
    }

    @Override
    public AiUpdateDocument findAiUpdateById(String id) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("AI Update를 찾을 수 없습니다: " + id);
        }

        return aiUpdateRepository.findById(objectId)
            .orElseThrow(() -> new ResourceNotFoundException("AI Update를 찾을 수 없습니다: " + id));
    }

    @Override
    public Page<AiUpdateDocument> searchAiUpdate(String query, Pageable pageable) {
        return aiUpdateRepository.findByTitleContainingIgnoreCase(query, pageable);
    }

    @Override
    public AiUpdateDocument saveAiUpdate(AiUpdateCreateRequest request) {
        // 중복 체크 (externalId 기준) - 이미 존재하면 기존 데이터 반환
        if (request.externalId() != null) {
            var existing = aiUpdateRepository.findByExternalId(request.externalId());
            if (existing.isPresent()) {
                log.debug("이미 존재하는 AI Update: externalId={}", request.externalId());
                return existing.get();
            }
        }

        AiUpdateDocument document = new AiUpdateDocument();
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
            AiUpdateDocument.AiUpdateMetadata metadata = new AiUpdateDocument.AiUpdateMetadata();
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

        return aiUpdateRepository.save(document);
    }

    @Override
    public AiUpdateDocument updateStatus(String id, String status) {
        AiUpdateDocument document = findAiUpdateById(id);
        document.setStatus(status);
        document.setUpdatedAt(LocalDateTime.now());
        return aiUpdateRepository.save(document);
    }
}
