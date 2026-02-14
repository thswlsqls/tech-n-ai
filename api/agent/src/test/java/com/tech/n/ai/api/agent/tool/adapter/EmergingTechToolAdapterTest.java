package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.EmergingTechDetailDto;
import com.tech.n.ai.api.agent.tool.dto.EmergingTechDto;
import com.tech.n.ai.api.agent.tool.dto.EmergingTechListDto;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import com.tech.n.ai.common.core.dto.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * EmergingTechToolAdapter 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergingTechToolAdapter 단위 테스트")
class EmergingTechToolAdapterTest {

    @Mock
    private EmergingTechInternalContract emergingTechContract;

    private EmergingTechToolAdapter adapter;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new EmergingTechToolAdapter(emergingTechContract, objectMapper);
        ReflectionTestUtils.setField(adapter, "apiKey", "test-api-key");
    }

    // ========== search 테스트 ==========

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("정상 검색 - 결과 목록 반환")
        void search_정상검색() {
            // Given
            Map<String, Object> item1 = createItemMap("id1", "OPENAI", "MODEL_RELEASE", "Title1");
            Map<String, Object> item2 = createItemMap("id2", "ANTHROPIC", "SDK_RELEASE", "Title2");
            Map<String, Object> data = Map.of("items", List.of(item1, item2));
            ApiResponse<Object> response = ApiResponse.success(data);

            when(emergingTechContract.searchEmergingTech(anyString(), anyString(), any(), anyInt(), anyInt()))
                    .thenReturn(response);

            // When
            List<EmergingTechDto> result = adapter.search("GPT", "OPENAI");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).provider()).isEqualTo("OPENAI");
        }

        @Test
        @DisplayName("provider 필터 빈 값 - 전체 검색")
        void search_provider빈값() {
            // Given
            Map<String, Object> item = createItemMap("id1", "GOOGLE", "API_UPDATE", "Title");
            Map<String, Object> data = Map.of("items", List.of(item));
            ApiResponse<Object> response = ApiResponse.success(data);

            when(emergingTechContract.searchEmergingTech(anyString(), anyString(), isNull(), anyInt(), anyInt()))
                    .thenReturn(response);

            // When
            List<EmergingTechDto> result = adapter.search("query", "");

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("API 실패 시 빈 리스트 반환")
        void search_API실패() {
            // Given
            when(emergingTechContract.searchEmergingTech(any(), any(), any(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("API 오류"));

            // When
            List<EmergingTechDto> result = adapter.search("query", "OPENAI");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("응답 code가 2000이 아니면 빈 리스트")
        void search_실패코드() {
            // Given
            ApiResponse<Object> response = new ApiResponse<>("4000", null, "실패", null);
            when(emergingTechContract.searchEmergingTech(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(response);

            // When
            List<EmergingTechDto> result = adapter.search("query", null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("data null 시 빈 리스트")
        void search_data_null() {
            // Given
            ApiResponse<Object> response = ApiResponse.success(null);
            when(emergingTechContract.searchEmergingTech(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(response);

            // When
            List<EmergingTechDto> result = adapter.search("query", null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("items null 시 빈 리스트")
        void search_items_null() {
            // Given
            Map<String, Object> data = Map.of("totalCount", 0);
            ApiResponse<Object> response = ApiResponse.success(data);
            when(emergingTechContract.searchEmergingTech(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(response);

            // When
            List<EmergingTechDto> result = adapter.search("query", null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ========== list 테스트 ==========

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("정상 조회 - 페이징 정보 포함")
        void list_정상조회() {
            // Given
            Map<String, Object> item = createItemMap("id1", "OPENAI", "MODEL_RELEASE", "Title");
            Map<String, Object> data = Map.of(
                    "items", List.of(item),
                    "totalCount", 100,
                    "pageNumber", 1,
                    "pageSize", 20
            );
            ApiResponse<Object> response = ApiResponse.success(data);

            when(emergingTechContract.listEmergingTechs(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(response);

            // When
            EmergingTechListDto result = adapter.list("2024-01-01", "2024-01-31",
                    "OPENAI", "MODEL_RELEASE", "GITHUB_RELEASE", "PUBLISHED", 1, 20);

            // Then
            assertThat(result.totalCount()).isEqualTo(100);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.items()).hasSize(1);
        }

        @Test
        @DisplayName("빈 필터 - null 변환")
        void list_빈필터() {
            // Given
            Map<String, Object> data = Map.of("items", List.of(), "totalCount", 0, "pageNumber", 1, "pageSize", 20);
            ApiResponse<Object> response = ApiResponse.success(data);

            when(emergingTechContract.listEmergingTechs(any(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt(), any()))
                    .thenReturn(response);

            // When
            EmergingTechListDto result = adapter.list("", "", "", "", "", "", 1, 20);

            // Then
            assertThat(result.items()).isEmpty();
            assertThat(result.period()).isEqualTo("전체");
        }

        @Test
        @DisplayName("API 실패 시 empty DTO 반환")
        void list_API실패() {
            // Given
            when(emergingTechContract.listEmergingTechs(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenThrow(new RuntimeException("API 오류"));

            // When
            EmergingTechListDto result = adapter.list("", "", "", "", "", "", 1, 20);

            // Then
            assertThat(result.totalCount()).isZero();
            assertThat(result.items()).isEmpty();
        }

        @Test
        @DisplayName("응답 실패 시 empty DTO 반환")
        void list_응답실패() {
            // Given
            ApiResponse<Object> response = new ApiResponse<>("5000", null, "서버 오류", null);
            when(emergingTechContract.listEmergingTechs(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(response);

            // When
            EmergingTechListDto result = adapter.list("", "", "", "", "", "", 1, 20);

            // Then
            assertThat(result.totalCount()).isZero();
        }

        @Test
        @DisplayName("period 문자열 생성 - 시작일만 있을 때")
        void list_period_시작일만() {
            // Given
            Map<String, Object> data = Map.of("items", List.of(), "totalCount", 0, "pageNumber", 1, "pageSize", 20);
            ApiResponse<Object> response = ApiResponse.success(data);
            when(emergingTechContract.listEmergingTechs(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(response);

            // When
            EmergingTechListDto result = adapter.list("2024-01-01", "", "", "", "", "", 1, 20);

            // Then
            assertThat(result.period()).contains("2024-01-01");
        }
    }

    // ========== getDetail 테스트 ==========

    @Nested
    @DisplayName("getDetail")
    class GetDetail {

        @Test
        @DisplayName("정상 조회 - 상세 정보 반환")
        void getDetail_정상조회() {
            // Given
            Map<String, Object> data = Map.of(
                    "id", "507f1f77bcf86cd799439011",
                    "provider", "OPENAI",
                    "updateType", "MODEL_RELEASE",
                    "title", "GPT-5 Release",
                    "summary", "New model release",
                    "url", "https://openai.com/blog/gpt-5",
                    "publishedAt", "2024-01-15",
                    "sourceType", "RSS",
                    "status", "PUBLISHED"
            );
            ApiResponse<Object> response = ApiResponse.success(data);

            when(emergingTechContract.getEmergingTechDetail(anyString(), anyString()))
                    .thenReturn(response);

            // When
            EmergingTechDetailDto result = adapter.getDetail("507f1f77bcf86cd799439011");

            // Then
            assertThat(result.id()).isEqualTo("507f1f77bcf86cd799439011");
            assertThat(result.provider()).isEqualTo("OPENAI");
            assertThat(result.title()).isEqualTo("GPT-5 Release");
        }

        @Test
        @DisplayName("API 실패 시 notFound 반환")
        void getDetail_API실패() {
            // Given
            when(emergingTechContract.getEmergingTechDetail(any(), any()))
                    .thenThrow(new RuntimeException("API 오류"));

            // When
            EmergingTechDetailDto result = adapter.getDetail("invalid-id");

            // Then
            assertThat(result.provider()).isNull();
            assertThat(result.summary()).contains("찾을 수 없습니다");
        }

        @Test
        @DisplayName("응답 실패 시 notFound 반환")
        void getDetail_응답실패() {
            // Given
            ApiResponse<Object> response = new ApiResponse<>("4040", null, "Not Found", null);
            when(emergingTechContract.getEmergingTechDetail(any(), any()))
                    .thenReturn(response);

            // When
            EmergingTechDetailDto result = adapter.getDetail("unknown-id");

            // Then
            assertThat(result.provider()).isNull();
            assertThat(result.summary()).contains("찾을 수 없습니다");
        }

        @Test
        @DisplayName("metadata 포함된 상세 정보")
        void getDetail_metadata포함() {
            // Given
            Map<String, Object> metadata = Map.of(
                    "version", "1.0.0",
                    "tags", List.of("AI", "LLM"),
                    "author", "OpenAI"
            );
            Map<String, Object> data = Map.of(
                    "id", "id1",
                    "provider", "OPENAI",
                    "updateType", "SDK_RELEASE",
                    "title", "SDK Update",
                    "metadata", metadata
            );
            ApiResponse<Object> response = ApiResponse.success(data);

            when(emergingTechContract.getEmergingTechDetail(any(), any()))
                    .thenReturn(response);

            // When
            EmergingTechDetailDto result = adapter.getDetail("id1");

            // Then
            assertThat(result.metadata()).isNotNull();
            assertThat(result.metadata().version()).isEqualTo("1.0.0");
            assertThat(result.metadata().tags()).contains("AI", "LLM");
        }
    }

    // ========== 헬퍼 메서드 ==========

    private Map<String, Object> createItemMap(String id, String provider, String updateType, String title) {
        return Map.of(
                "id", id,
                "provider", provider,
                "updateType", updateType,
                "title", title,
                "url", "https://example.com/" + id,
                "status", "PUBLISHED"
        );
    }
}
