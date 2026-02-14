package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.LLMService;
import com.tech.n.ai.api.chatbot.service.PromptService;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnswerGenerationChain 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerGenerationChain 단위 테스트")
class AnswerGenerationChainTest {

    @Mock
    private PromptService promptService;

    @Mock
    private LLMService llmService;

    @InjectMocks
    private AnswerGenerationChain answerChain;

    // ========== generate 테스트 ==========

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("정상 답변 생성")
        void generate_성공() {
            // Given
            String query = "AI 트렌드 알려줘";
            List<SearchResult> searchResults = List.of(
                createSearchResult("doc1", "AI 관련 기사 내용", 0.9)
            );
            String builtPrompt = "프롬프트 내용";
            String llmResponse = "AI 트렌드에 대한 답변입니다.";

            when(promptService.buildPrompt(query, searchResults)).thenReturn(builtPrompt);
            when(llmService.generate(builtPrompt)).thenReturn(llmResponse);

            // When
            String result = answerChain.generate(query, searchResults);

            // Then
            assertThat(result).isEqualTo(llmResponse);
            verify(promptService).buildPrompt(query, searchResults);
            verify(llmService).generate(builtPrompt);
        }

        @Test
        @DisplayName("빈 검색 결과로 답변 생성")
        void generate_빈_검색결과() {
            // Given
            String query = "테스트 쿼리";
            List<SearchResult> emptyResults = List.of();
            String builtPrompt = "검색 결과 없음 프롬프트";
            String llmResponse = "관련 정보를 찾을 수 없습니다.";

            when(promptService.buildPrompt(query, emptyResults)).thenReturn(builtPrompt);
            when(llmService.generate(builtPrompt)).thenReturn(llmResponse);

            // When
            String result = answerChain.generate(query, emptyResults);

            // Then
            assertThat(result).isEqualTo(llmResponse);
        }
    }

    // ========== 후처리 테스트 ==========

    @Nested
    @DisplayName("generate - 후처리")
    class PostProcessing {

        @Test
        @DisplayName("앞뒤 공백 제거")
        void generate_trimWhitespace() {
            // Given
            when(promptService.buildPrompt(anyString(), anyList())).thenReturn("prompt");
            when(llmService.generate(anyString())).thenReturn("  답변 내용  ");

            // When
            String result = answerChain.generate("쿼리", List.of());

            // Then
            assertThat(result).isEqualTo("답변 내용");
        }

        @Test
        @DisplayName("답변: 접두사 제거")
        void generate_remove답변Prefix() {
            // Given
            when(promptService.buildPrompt(anyString(), anyList())).thenReturn("prompt");
            when(llmService.generate(anyString())).thenReturn("답변: 실제 답변 내용");

            // When
            String result = answerChain.generate("쿼리", List.of());

            // Then
            assertThat(result).isEqualTo("실제 답변 내용");
        }

        @Test
        @DisplayName("응답: 접두사 제거")
        void generate_remove응답Prefix() {
            // Given
            when(promptService.buildPrompt(anyString(), anyList())).thenReturn("prompt");
            when(llmService.generate(anyString())).thenReturn("응답: 실제 응답 내용");

            // When
            String result = answerChain.generate("쿼리", List.of());

            // Then
            assertThat(result).isEqualTo("실제 응답 내용");
        }

        @Test
        @DisplayName("Answer: 영문 접두사 제거")
        void generate_removeAnswerPrefix() {
            // Given
            when(promptService.buildPrompt(anyString(), anyList())).thenReturn("prompt");
            when(llmService.generate(anyString())).thenReturn("Answer: The actual answer content");

            // When
            String result = answerChain.generate("query", List.of());

            // Then
            assertThat(result).isEqualTo("The actual answer content");
        }

        @Test
        @DisplayName("Response: 영문 접두사 제거")
        void generate_removeResponsePrefix() {
            // Given
            when(promptService.buildPrompt(anyString(), anyList())).thenReturn("prompt");
            when(llmService.generate(anyString())).thenReturn("Response: The actual response");

            // When
            String result = answerChain.generate("query", List.of());

            // Then
            assertThat(result).isEqualTo("The actual response");
        }

        @Test
        @DisplayName("접두사 없는 응답은 그대로 유지")
        void generate_noPrefixUnchanged() {
            // Given
            when(promptService.buildPrompt(anyString(), anyList())).thenReturn("prompt");
            when(llmService.generate(anyString())).thenReturn("일반 답변 내용입니다.");

            // When
            String result = answerChain.generate("쿼리", List.of());

            // Then
            assertThat(result).isEqualTo("일반 답변 내용입니다.");
        }
    }

    // ========== 헬퍼 메서드 ==========

    private SearchResult createSearchResult(String documentId, String text, double score) {
        return SearchResult.builder()
            .documentId(documentId)
            .text(text)
            .score(score)
            .collectionType("BOOKMARK")
            .build();
    }
}
