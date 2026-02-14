package com.tech.n.ai.api.emergingtech.service;

import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import com.tech.n.ai.domain.mongodb.repository.EmergingTechRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * EmergingTechQueryService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergingTechQueryService 단위 테스트")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EmergingTechQueryServiceTest {

    @Mock
    private EmergingTechRepository emergingTechRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private EmergingTechQueryServiceImpl queryService;

    // ========== findEmergingTechs 테스트 ==========

    @Nested
    @DisplayName("findEmergingTechs")
    class FindEmergingTechs {

        @Test
        @DisplayName("필터 없이 조회")
        void findEmergingTechs_필터없음() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<EmergingTechDocument> documents = List.of(createDocument(), createDocument());

            when(mongoTemplate.find(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(documents);
            when(mongoTemplate.count(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(2L);

            // When
            Page<EmergingTechDocument> result = queryService.findEmergingTechs(
                null, null, null, null, null, null, pageable
            );

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("provider 필터 적용")
        void findEmergingTechs_provider필터() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            EmergingTechDocument doc = createDocument();
            doc.setProvider("GITHUB");

            when(mongoTemplate.find(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(List.of(doc));
            when(mongoTemplate.count(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(1L);

            // When
            Page<EmergingTechDocument> result = queryService.findEmergingTechs(
                "GITHUB", null, null, null, null, null, pageable
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getProvider()).isEqualTo("GITHUB");
        }

        @Test
        @DisplayName("복합 필터 적용")
        void findEmergingTechs_복합필터() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            EmergingTechDocument doc = createDocument();

            when(mongoTemplate.find(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(List.of(doc));
            when(mongoTemplate.count(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(1L);

            // When
            Page<EmergingTechDocument> result = queryService.findEmergingTechs(
                "GITHUB", "FRAMEWORK_UPDATE", "PENDING", "RSS", null, null, pageable
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("날짜 범위 필터 적용")
        void findEmergingTechs_날짜필터() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            when(mongoTemplate.find(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(List.of());
            when(mongoTemplate.count(any(Query.class), eq(EmergingTechDocument.class)))
                .thenReturn(0L);

            // When
            Page<EmergingTechDocument> result = queryService.findEmergingTechs(
                null, null, null, null, "2024-01-01", "2024-12-31", pageable
            );

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ========== findEmergingTechById 테스트 ==========

    @Nested
    @DisplayName("findEmergingTechById")
    class FindEmergingTechById {

        @Test
        @DisplayName("정상 조회")
        void findEmergingTechById_성공() {
            // Given
            ObjectId id = new ObjectId();
            EmergingTechDocument document = createDocument();
            document.setId(id);

            when(emergingTechRepository.findById(id)).thenReturn(Optional.of(document));

            // When
            EmergingTechDocument result = queryService.findEmergingTechById(id.toHexString());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("존재하지 않는 ID - ResourceNotFoundException")
        void findEmergingTechById_미존재() {
            // Given
            ObjectId id = new ObjectId();

            when(emergingTechRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> queryService.findEmergingTechById(id.toHexString()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Emerging Tech를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("잘못된 ID 형식 - ResourceNotFoundException")
        void findEmergingTechById_잘못된_형식() {
            // Given
            String invalidId = "invalid-id";

            // When & Then
            assertThatThrownBy(() -> queryService.findEmergingTechById(invalidId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Emerging Tech를 찾을 수 없습니다");
        }
    }

    // ========== searchEmergingTech 테스트 ==========

    @Nested
    @DisplayName("searchEmergingTech")
    class SearchEmergingTech {

        @Test
        @DisplayName("제목 키워드 검색")
        void searchEmergingTech_성공() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            EmergingTechDocument doc = createDocument();
            doc.setTitle("Spring Boot 3.0 Release");
            Page<EmergingTechDocument> page = new PageImpl<>(List.of(doc));

            when(emergingTechRepository.findByTitleContainingIgnoreCase("Spring", pageable))
                .thenReturn(page);

            // When
            Page<EmergingTechDocument> result = queryService.searchEmergingTech("Spring", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("Spring");
        }

        @Test
        @DisplayName("검색 결과 없음")
        void searchEmergingTech_결과없음() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<EmergingTechDocument> page = new PageImpl<>(List.of());

            when(emergingTechRepository.findByTitleContainingIgnoreCase("없는검색어", pageable))
                .thenReturn(page);

            // When
            Page<EmergingTechDocument> result = queryService.searchEmergingTech("없는검색어", pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ========== 헬퍼 메서드 ==========

    private EmergingTechDocument createDocument() {
        EmergingTechDocument doc = new EmergingTechDocument();
        doc.setId(new ObjectId());
        doc.setProvider("GITHUB");
        doc.setUpdateType("FRAMEWORK_UPDATE");
        doc.setTitle("Test Title");
        doc.setUrl("https://example.com");
        doc.setStatus("PENDING");
        doc.setSourceType("RSS");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }
}
