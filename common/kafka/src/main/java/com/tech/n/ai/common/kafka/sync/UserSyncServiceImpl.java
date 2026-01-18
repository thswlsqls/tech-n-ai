package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.UserCreatedEvent;
import com.tech.n.ai.common.kafka.event.UserDeletedEvent;
import com.tech.n.ai.common.kafka.event.UserRestoredEvent;
import com.tech.n.ai.common.kafka.event.UserUpdatedEvent;
import com.tech.n.ai.datasource.mongodb.document.UserProfileDocument;
import com.tech.n.ai.datasource.mongodb.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * User 동기화 서비스 구현 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncServiceImpl implements UserSyncService {
    
    private final UserProfileRepository userProfileRepository;
    
    @Override
    public void syncUserCreated(UserCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: userTsid로 조회하여 없으면 생성, 있으면 업데이트
            UserProfileDocument document = userProfileRepository
                .findByUserTsid(payload.userTsid())
                .orElse(new UserProfileDocument());
            
            document.setUserTsid(payload.userTsid());
            document.setUserId(payload.userId());
            document.setUsername(payload.username());
            document.setEmail(payload.email());
            document.setProfileImageUrl(payload.profileImageUrl());
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(document);
            
            log.debug("Successfully synced UserCreatedEvent: userTsid={}, userId={}", 
                payload.userTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync UserCreatedEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserCreatedEvent", e);
        }
    }
    
    @Override
    public void syncUserUpdated(UserUpdatedEvent event) {
        try {
            var payload = event.payload();
            var updatedFields = payload.updatedFields();
            
            // userTsid로 Document 조회
            UserProfileDocument document = userProfileRepository
                .findByUserTsid(payload.userTsid())
                .orElseThrow(() -> new RuntimeException(
                    "UserProfileDocument not found: userTsid=" + payload.userTsid()));
            
            // updatedFields를 Document 필드에 매핑 (부분 업데이트)
            updateDocumentFields(document, updatedFields);
            document.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(document);
            
            log.debug("Successfully synced UserUpdatedEvent: userTsid={}, updatedFields={}", 
                payload.userTsid(), updatedFields.keySet());
        } catch (Exception e) {
            log.error("Failed to sync UserUpdatedEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserUpdatedEvent", e);
        }
    }
    
    @Override
    public void syncUserDeleted(UserDeletedEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제
            userProfileRepository.deleteByUserTsid(payload.userTsid());
            
            log.debug("Successfully synced UserDeletedEvent: userTsid={}, userId={}", 
                payload.userTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync UserDeletedEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserDeletedEvent", e);
        }
    }
    
    @Override
    public void syncUserRestored(UserRestoredEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
            UserProfileDocument document = new UserProfileDocument();
            document.setUserTsid(payload.userTsid());
            document.setUserId(payload.userId());
            document.setUsername(payload.username());
            document.setEmail(payload.email());
            document.setProfileImageUrl(payload.profileImageUrl());
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(document);
            
            log.debug("Successfully synced UserRestoredEvent: userTsid={}, userId={}", 
                payload.userTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync UserRestoredEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserRestoredEvent", e);
        }
    }
    
    /**
     * updatedFields를 Document 필드에 매핑 (부분 업데이트)
     * 
     * @param document 대상 Document
     * @param updatedFields 업데이트할 필드 맵
     */
    private void updateDocumentFields(UserProfileDocument document, Map<String, Object> updatedFields) {
        for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            try {
                switch (fieldName) {
                    case "username":
                        document.setUsername((String) value);
                        break;
                    case "email":
                        document.setEmail((String) value);
                        break;
                    case "profileImageUrl":
                        document.setProfileImageUrl((String) value);
                        break;
                    default:
                        log.warn("Unknown field in updatedFields: {}", fieldName);
                }
            } catch (ClassCastException e) {
                log.warn("Type mismatch for field {}: expected String, got {}", fieldName, value.getClass().getName());
            }
        }
    }
}
