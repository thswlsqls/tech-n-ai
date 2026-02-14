package com.tech.n.ai.api.emergingtech.controller;

import com.tech.n.ai.api.emergingtech.common.InternalApiKeyValidator;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechBatchRequest;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechBatchResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechDetailResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechPageResponse;
import com.tech.n.ai.api.emergingtech.facade.EmergingTechFacade;
import com.tech.n.ai.common.core.dto.PageData;
import tools.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmergingTechController 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergingTechController 단위 테스트")
class EmergingTechControllerTest {

    @Mock
    private EmergingTechFacade emergingTechFacade;

    @Mock
    private InternalApiKeyValidator apiKeyValidator;

    @InjectMocks
    private EmergingTechController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/emerging-tech";
    private static final String VALID_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();
    }

    // ========== GET /emerging-tech 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/emerging-tech")
    class GetEmergingTechList {

        @Test
        @DisplayName("목록 조회 - 200 OK")
        void getEmergingTechList_성공() throws Exception {
            EmergingTechPageResponse response = createPageResponse();
            when(emergingTechFacade.getEmergingTechList(any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.items").exists());
        }

        @Test
        @DisplayName("기본값 적용 조회 - 200 OK")
        void getEmergingTechList_기본값() throws Exception {
            EmergingTechPageResponse response = createPageResponse();
            when(emergingTechFacade.getEmergingTechList(any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("필터 적용 조회 - 200 OK")
        void getEmergingTechList_필터() throws Exception {
            EmergingTechPageResponse response = createPageResponse();
            when(emergingTechFacade.getEmergingTechList(any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                    .param("provider", "GITHUB")
                    .param("status", "PENDING"))
                .andExpect(status().isOk());
        }
    }

    // ========== GET /emerging-tech/{id} 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/emerging-tech/{id}")
    class GetEmergingTechDetail {

        @Test
        @DisplayName("상세 조회 - 200 OK")
        void getEmergingTechDetail_성공() throws Exception {
            String id = new ObjectId().toHexString();
            EmergingTechDetailResponse response = createDetailResponse(id);
            when(emergingTechFacade.getEmergingTechDetail(id)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.id").value(id));
        }
    }

    // ========== GET /emerging-tech/search 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/emerging-tech/search")
    class SearchEmergingTech {

        @Test
        @DisplayName("검색 - 200 OK")
        void searchEmergingTech_성공() throws Exception {
            EmergingTechPageResponse response = createPageResponse();
            when(emergingTechFacade.searchEmergingTech(any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/search")
                    .param("q", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }

        @Test
        @DisplayName("검색어 누락 - 400 Bad Request")
        void searchEmergingTech_검색어_누락() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search"))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /emerging-tech/internal 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/emerging-tech/internal")
    class CreateEmergingTechInternal {

        @Test
        @DisplayName("단건 생성 - 200 OK")
        void createEmergingTechInternal_성공() throws Exception {
            EmergingTechCreateRequest request = createRequest();
            String id = new ObjectId().toHexString();
            EmergingTechDetailResponse response = createDetailResponse(id);

            doNothing().when(apiKeyValidator).validate(VALID_API_KEY);
            when(emergingTechFacade.createEmergingTech(any())).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/internal")
                    .header("X-Internal-Api-Key", VALID_API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.id").value(id));
        }

        @Test
        @DisplayName("API 키 누락 - 400 Bad Request")
        void createEmergingTechInternal_API키_누락() throws Exception {
            EmergingTechCreateRequest request = createRequest();

            mockMvc.perform(post(BASE_URL + "/internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("필수 필드 누락 - 400 Bad Request")
        void createEmergingTechInternal_필수필드_누락() throws Exception {
            String body = """
                {"title": "Test"}
                """;

            mockMvc.perform(post(BASE_URL + "/internal")
                    .header("X-Internal-Api-Key", VALID_API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /emerging-tech/internal/batch 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/emerging-tech/internal/batch")
    class CreateEmergingTechBatchInternal {

        @Test
        @DisplayName("다건 생성 - 200 OK")
        void createEmergingTechBatchInternal_성공() throws Exception {
            EmergingTechBatchRequest request = new EmergingTechBatchRequest(
                List.of(createRequest(), createRequest()));

            EmergingTechBatchResponse response = EmergingTechBatchResponse.builder()
                .totalCount(2)
                .successCount(2)
                .newCount(2)
                .duplicateCount(0)
                .failureCount(0)
                .failureMessages(List.of())
                .build();

            doNothing().when(apiKeyValidator).validate(VALID_API_KEY);
            when(emergingTechFacade.createEmergingTechBatch(any())).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/internal/batch")
                    .header("X-Internal-Api-Key", VALID_API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.totalCount").value(2));
        }
    }

    // ========== POST /emerging-tech/{id}/approve 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/emerging-tech/{id}/approve")
    class ApproveEmergingTech {

        @Test
        @DisplayName("승인 - 200 OK")
        void approveEmergingTech_성공() throws Exception {
            String id = new ObjectId().toHexString();
            EmergingTechDetailResponse response = createDetailResponse(id);

            doNothing().when(apiKeyValidator).validate(VALID_API_KEY);
            when(emergingTechFacade.approveEmergingTech(id)).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/" + id + "/approve")
                    .header("X-Internal-Api-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }
    }

    // ========== POST /emerging-tech/{id}/reject 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/emerging-tech/{id}/reject")
    class RejectEmergingTech {

        @Test
        @DisplayName("거부 - 200 OK")
        void rejectEmergingTech_성공() throws Exception {
            String id = new ObjectId().toHexString();
            EmergingTechDetailResponse response = createDetailResponse(id);

            doNothing().when(apiKeyValidator).validate(VALID_API_KEY);
            when(emergingTechFacade.rejectEmergingTech(id)).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/" + id + "/reject")
                    .header("X-Internal-Api-Key", VALID_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }
    }

    // ========== 헬퍼 메서드 ==========

    private EmergingTechCreateRequest createRequest() {
        return EmergingTechCreateRequest.builder()
            .provider("GITHUB")
            .updateType("FRAMEWORK_UPDATE")
            .title("Test Title")
            .summary("Test Summary")
            .url("https://example.com")
            .publishedAt(LocalDateTime.now())
            .sourceType("RSS")
            .status("PENDING")
            .build();
    }

    private EmergingTechDetailResponse createDetailResponse(String id) {
        return EmergingTechDetailResponse.builder()
            .id(id)
            .provider("GITHUB")
            .updateType("FRAMEWORK_UPDATE")
            .title("Test Title")
            .summary("Test Summary")
            .url("https://example.com")
            .publishedAt(LocalDateTime.now())
            .sourceType("RSS")
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private EmergingTechPageResponse createPageResponse() {
        PageData<EmergingTechDetailResponse> pageData = PageData.of(10, 1, 0, List.of());
        return EmergingTechPageResponse.from(pageData);
    }
}
