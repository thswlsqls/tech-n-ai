package com.tech.n.ai.api.emergingtech.service;

import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import com.tech.n.ai.domain.mongodb.enums.PostStatus;
import com.tech.n.ai.domain.mongodb.repository.EmergingTechRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmergingTechCommandService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergingTechCommandService 단위 테스트")
class EmergingTechCommandServiceTest {

    @Mock
    private EmergingTechRepository emergingTechRepository;

    @Mock
    private EmergingTechQueryService queryService;

    @InjectMocks
    private EmergingTechCommandServiceImpl commandService;

    // ========== saveEmergingTech 테스트 ==========

    @Nested
    @DisplayName("saveEmergingTech")
    class SaveEmergingTech {

        @Test
        @DisplayName("신규 저장 - SaveResult(isNew=true) 반환")
        void saveEmergingTech_신규() {
            // Given
            EmergingTechCreateRequest request = createRequest("ext-123", "https://example.com/new");

            when(emergingTechRepository.findByExternalId("ext-123")).thenReturn(Optional.empty());
            when(emergingTechRepository.findByUrl("https://example.com/new")).thenReturn(Optional.empty());
            when(emergingTechRepository.save(any(EmergingTechDocument.class))).thenAnswer(invocation -> {
                EmergingTechDocument doc = invocation.getArgument(0);
                doc.setId(new ObjectId());
                return doc;
            });

            // When
            EmergingTechCommandService.SaveResult result = commandService.saveEmergingTech(request);

            // Then
            assertThat(result.isNew()).isTrue();
            assertThat(result.document()).isNotNull();
            assertThat(result.document().getTitle()).isEqualTo("Test Title");
            verify(emergingTechRepository).save(any(EmergingTechDocument.class));
        }

        @Test
        @DisplayName("externalId 중복 시 - SaveResult(isNew=false) 반환")
        void saveEmergingTech_중복_externalId() {
            // Given
            EmergingTechCreateRequest request = createRequest("ext-123", "https://example.com/new");
            EmergingTechDocument existing = createDocument();

            when(emergingTechRepository.findByExternalId("ext-123")).thenReturn(Optional.of(existing));

            // When
            EmergingTechCommandService.SaveResult result = commandService.saveEmergingTech(request);

            // Then
            assertThat(result.isNew()).isFalse();
            assertThat(result.document()).isEqualTo(existing);
            verify(emergingTechRepository, never()).save(any());
        }

        @Test
        @DisplayName("url 중복 시 - SaveResult(isNew=false) 반환")
        void saveEmergingTech_중복_url() {
            // Given
            EmergingTechCreateRequest request = createRequest(null, "https://example.com/existing");
            EmergingTechDocument existing = createDocument();

            when(emergingTechRepository.findByUrl("https://example.com/existing")).thenReturn(Optional.of(existing));

            // When
            EmergingTechCommandService.SaveResult result = commandService.saveEmergingTech(request);

            // Then
            assertThat(result.isNew()).isFalse();
            assertThat(result.document()).isEqualTo(existing);
            verify(emergingTechRepository, never()).save(any());
        }

        @Test
        @DisplayName("externalId 없이 신규 저장")
        void saveEmergingTech_externalId_없이_신규() {
            // Given
            EmergingTechCreateRequest request = createRequest(null, "https://example.com/new");

            when(emergingTechRepository.findByUrl("https://example.com/new")).thenReturn(Optional.empty());
            when(emergingTechRepository.save(any(EmergingTechDocument.class))).thenAnswer(invocation -> {
                EmergingTechDocument doc = invocation.getArgument(0);
                doc.setId(new ObjectId());
                return doc;
            });

            // When
            EmergingTechCommandService.SaveResult result = commandService.saveEmergingTech(request);

            // Then
            assertThat(result.isNew()).isTrue();
            verify(emergingTechRepository, never()).findByExternalId(any());
            verify(emergingTechRepository).save(any(EmergingTechDocument.class));
        }
    }

    // ========== updateStatus 테스트 ==========

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("PUBLISHED로 상태 변경 성공")
        void updateStatus_승인() {
            // Given
            String id = new ObjectId().toHexString();
            EmergingTechDocument document = createDocument();

            when(queryService.findEmergingTechById(id)).thenReturn(document);
            when(emergingTechRepository.save(any(EmergingTechDocument.class))).thenReturn(document);

            // When
            EmergingTechDocument result = commandService.updateStatus(id, PostStatus.PUBLISHED);

            // Then
            assertThat(result.getStatus()).isEqualTo(PostStatus.PUBLISHED.name());
            verify(emergingTechRepository).save(document);
        }

        @Test
        @DisplayName("REJECTED로 상태 변경 성공")
        void updateStatus_거부() {
            // Given
            String id = new ObjectId().toHexString();
            EmergingTechDocument document = createDocument();

            when(queryService.findEmergingTechById(id)).thenReturn(document);
            when(emergingTechRepository.save(any(EmergingTechDocument.class))).thenReturn(document);

            // When
            EmergingTechDocument result = commandService.updateStatus(id, PostStatus.REJECTED);

            // Then
            assertThat(result.getStatus()).isEqualTo(PostStatus.REJECTED.name());
        }

        @Test
        @DisplayName("존재하지 않는 ID - ResourceNotFoundException")
        void updateStatus_미존재() {
            // Given
            String id = new ObjectId().toHexString();

            when(queryService.findEmergingTechById(id))
                .thenThrow(new ResourceNotFoundException("Emerging Tech를 찾을 수 없습니다: " + id));

            // When & Then
            assertThatThrownBy(() -> commandService.updateStatus(id, PostStatus.PUBLISHED))
                .isInstanceOf(ResourceNotFoundException.class);
            verify(emergingTechRepository, never()).save(any());
        }
    }

    // ========== 헬퍼 메서드 ==========

    private EmergingTechCreateRequest createRequest(String externalId, String url) {
        return EmergingTechCreateRequest.builder()
            .provider("GITHUB")
            .updateType("FRAMEWORK_UPDATE")
            .title("Test Title")
            .summary("Test Summary")
            .url(url)
            .publishedAt(LocalDateTime.now())
            .sourceType("RSS")
            .status("PENDING")
            .externalId(externalId)
            .build();
    }

    private EmergingTechDocument createDocument() {
        EmergingTechDocument doc = new EmergingTechDocument();
        doc.setId(new ObjectId());
        doc.setProvider("GITHUB");
        doc.setUpdateType("FRAMEWORK_UPDATE");
        doc.setTitle("Test Title");
        doc.setUrl("https://example.com");
        doc.setStatus("PENDING");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }
}
