package com.tech.n.ai.api.bookmark.controller;

import com.tech.n.ai.api.bookmark.dto.request.BookmarkCreateRequest;
import com.tech.n.ai.api.bookmark.dto.request.BookmarkUpdateRequest;
import com.tech.n.ai.api.bookmark.dto.response.BookmarkDetailResponse;
import com.tech.n.ai.api.bookmark.dto.response.BookmarkListResponse;
import com.tech.n.ai.api.bookmark.dto.response.BookmarkSearchResponse;
import com.tech.n.ai.api.bookmark.facade.BookmarkFacade;
import com.tech.n.ai.common.core.dto.PageData;
import com.tech.n.ai.common.security.principal.UserPrincipal;
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
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BookmarkController 단위 테스트
 *
 * standaloneSetup 사용하여 순수 Controller 로직만 테스트.
 * UserPrincipal은 커스텀 ArgumentResolver로 주입.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookmarkController 단위 테스트")
class BookmarkControllerTest {

    @Mock
    private BookmarkFacade bookmarkFacade;

    @InjectMocks
    private BookmarkController bookmarkController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final Long TEST_USER_ID = 1L;
    private static final String BASE_URL = "/api/v1/bookmark";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // standaloneSetup으로 커스텀 ArgumentResolver 설정
        mockMvc = MockMvcBuilders
            .standaloneSetup(bookmarkController)
            .setCustomArgumentResolvers(new TestUserPrincipalArgumentResolver())
            .build();
    }

    // ========== POST /bookmark 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/bookmark")
    class SaveBookmark {

        @Test
        @DisplayName("정상 저장 - 200 OK")
        void saveBookmark_성공() throws Exception {
            String emergingTechId = new ObjectId().toHexString();
            BookmarkCreateRequest request = new BookmarkCreateRequest(emergingTechId, List.of("AI"), "메모");
            BookmarkDetailResponse response = createDetailResponse("100", emergingTechId);

            when(bookmarkFacade.saveBookmark(eq(TEST_USER_ID), any(BookmarkCreateRequest.class)))
                .thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.bookmarkTsid").value("100"));
        }

        @Test
        @DisplayName("EmergingTech ID 누락 - 400 Bad Request")
        void saveBookmark_ID_누락() throws Exception {
            String body = """
                {"tag": "AI", "memo": "메모"}
                """;

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 EmergingTech ID - 400 Bad Request")
        void saveBookmark_빈_ID() throws Exception {
            BookmarkCreateRequest request = new BookmarkCreateRequest("", List.of("AI"), "메모");

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== GET /bookmark 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/bookmark")
    class GetBookmarkList {

        @Test
        @DisplayName("목록 조회 - 200 OK")
        void getBookmarkList_성공() throws Exception {
            BookmarkListResponse response = createListResponse();
            when(bookmarkFacade.getBookmarkList(eq(TEST_USER_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.data").exists());
        }

        @Test
        @DisplayName("기본값 적용 조회 - 200 OK")
        void getBookmarkList_기본값() throws Exception {
            BookmarkListResponse response = createListResponse();
            when(bookmarkFacade.getBookmarkList(eq(TEST_USER_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
        }
    }

    // ========== GET /bookmark/{id} 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/bookmark/{id}")
    class GetBookmarkDetail {

        @Test
        @DisplayName("상세 조회 - 200 OK")
        void getBookmarkDetail_성공() throws Exception {
            String emergingTechId = new ObjectId().toHexString();
            BookmarkDetailResponse response = createDetailResponse("100", emergingTechId);
            when(bookmarkFacade.getBookmarkDetail(TEST_USER_ID, "100")).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"))
                .andExpect(jsonPath("$.data.bookmarkTsid").value("100"));
        }
    }

    // ========== PUT /bookmark/{id} 테스트 ==========

    @Nested
    @DisplayName("PUT /api/v1/bookmark/{id}")
    class UpdateBookmark {

        @Test
        @DisplayName("수정 - 200 OK")
        void updateBookmark_성공() throws Exception {
            BookmarkUpdateRequest request = new BookmarkUpdateRequest(List.of("새태그"), "새메모");
            String emergingTechId = new ObjectId().toHexString();
            BookmarkDetailResponse response = createDetailResponse("100", emergingTechId);

            when(bookmarkFacade.updateBookmark(eq(TEST_USER_ID), eq("100"), any(BookmarkUpdateRequest.class)))
                .thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }
    }

    // ========== DELETE /bookmark/{id} 테스트 ==========

    @Nested
    @DisplayName("DELETE /api/v1/bookmark/{id}")
    class DeleteBookmark {

        @Test
        @DisplayName("삭제 - 200 OK")
        void deleteBookmark_성공() throws Exception {
            doNothing().when(bookmarkFacade).deleteBookmark(TEST_USER_ID, "100");

            mockMvc.perform(delete(BASE_URL + "/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));

            verify(bookmarkFacade).deleteBookmark(TEST_USER_ID, "100");
        }
    }

    // ========== GET /bookmark/search 테스트 ==========

    @Nested
    @DisplayName("GET /api/v1/bookmark/search")
    class SearchBookmarks {

        @Test
        @DisplayName("검색 - 200 OK")
        void searchBookmarks_성공() throws Exception {
            BookmarkSearchResponse response = createSearchResponse();
            when(bookmarkFacade.searchBookmarks(eq(TEST_USER_ID), any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/search")
                    .param("q", "AI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }

        @Test
        @DisplayName("검색어 누락 - 400 Bad Request")
        void searchBookmarks_검색어_누락() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search"))
                .andExpect(status().isBadRequest());
        }
    }

    // ========== POST /bookmark/{id}/restore 테스트 ==========

    @Nested
    @DisplayName("POST /api/v1/bookmark/{id}/restore")
    class RestoreBookmark {

        @Test
        @DisplayName("복구 - 200 OK")
        void restoreBookmark_성공() throws Exception {
            String emergingTechId = new ObjectId().toHexString();
            BookmarkDetailResponse response = createDetailResponse("100", emergingTechId);
            when(bookmarkFacade.restoreBookmark(TEST_USER_ID, "100")).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/100/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("2000"));
        }
    }

    // ========== 헬퍼 클래스 ==========

    /**
     * 테스트용 UserPrincipal ArgumentResolver
     */
    static class TestUserPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(UserPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return new UserPrincipal(TEST_USER_ID, "test@example.com", "USER");
        }
    }

    // ========== 헬퍼 메서드 ==========

    private BookmarkDetailResponse createDetailResponse(String bookmarkTsid, String emergingTechId) {
        return new BookmarkDetailResponse(
            bookmarkTsid,
            TEST_USER_ID.toString(),
            emergingTechId,
            "Test Title",
            "https://example.com",
            "test",
            "summary",
            LocalDateTime.now(),
            List.of("AI"),
            "memo",
            LocalDateTime.now(),
            TEST_USER_ID.toString(),
            LocalDateTime.now(),
            TEST_USER_ID.toString()
        );
    }

    private BookmarkListResponse createListResponse() {
        PageData<BookmarkDetailResponse> pageData = PageData.of(10, 1, 0, List.of());
        return BookmarkListResponse.from(pageData);
    }

    private BookmarkSearchResponse createSearchResponse() {
        PageData<BookmarkDetailResponse> pageData = PageData.of(10, 1, 0, List.of());
        return BookmarkSearchResponse.from(pageData);
    }
}
