package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.Intent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IntentClassificationService 단위 테스트
 *
 * 순수 비즈니스 로직 테스트 (외부 의존성 없음)
 */
@DisplayName("IntentClassificationService 단위 테스트")
class IntentClassificationServiceTest {

    private final IntentClassificationService intentService = new IntentClassificationServiceImpl();

    // ========== AGENT_COMMAND 테스트 ==========

    @Nested
    @DisplayName("classifyIntent - AGENT_COMMAND")
    class AgentCommand {

        @Test
        @DisplayName("@agent 프리픽스가 있으면 AGENT_COMMAND 반환")
        void classifyIntent_agent_prefix() {
            // Given
            String input = "@agent AI 트렌드 분석해줘";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.AGENT_COMMAND);
        }

        @Test
        @DisplayName("@agent 대소문자 무시")
        void classifyIntent_agent_caseInsensitive() {
            // Given
            String input = "@AGENT 작업 실행";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.AGENT_COMMAND);
        }

        @Test
        @DisplayName("@agent가 문장 중간에 있으면 AGENT_COMMAND 아님")
        void classifyIntent_agent_notPrefix() {
            // Given
            String input = "이것은 @agent 테스트입니다";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isNotEqualTo(Intent.AGENT_COMMAND);
        }
    }

    // ========== WEB_SEARCH_REQUIRED 테스트 ==========

    @Nested
    @DisplayName("classifyIntent - WEB_SEARCH_REQUIRED")
    class WebSearchRequired {

        @ParameterizedTest
        @DisplayName("웹 검색 키워드만 포함 시 WEB_SEARCH_REQUIRED 반환")
        @ValueSource(strings = {
            "오늘 날씨 어때?",
            "현재 비트코인 시세"
        })
        void classifyIntent_webSearchKeywords(String input) {
            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.WEB_SEARCH_REQUIRED);
        }

        @ParameterizedTest
        @DisplayName("웹 검색 + RAG 키워드 동시 포함 시 RAG 우선")
        @ValueSource(strings = {
            "지금 뉴스 알려줘",
            "최근 AI 트렌드",
            "today's news",
            "latest technology news"
        })
        void classifyIntent_webSearchWithRagKeywords_ragWins(String input) {
            // When
            Intent result = intentService.classifyIntent(input);

            // Then: RAG 키워드가 있으므로 RAG 우선
            assertThat(result).isEqualTo(Intent.RAG_REQUIRED);
        }

        @Test
        @DisplayName("실시간 정보 요청 시 WEB_SEARCH_REQUIRED 반환")
        void classifyIntent_realTimeInfo() {
            // Given: RAG 키워드 없는 순수 실시간 정보 요청
            String input = "실시간 주가 시세";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.WEB_SEARCH_REQUIRED);
        }
    }

    // ========== RAG_REQUIRED 테스트 ==========

    @Nested
    @DisplayName("classifyIntent - RAG_REQUIRED")
    class RagRequired {

        @ParameterizedTest
        @DisplayName("RAG 키워드 포함 시 RAG_REQUIRED 반환")
        @ValueSource(strings = {
            "대회 정보",
            "kaggle 대회 목록",
            "codeforces 관련",
            "AI 트렌드 분석",
            "openai 릴리즈"
        })
        void classifyIntent_ragKeywords(String input) {
            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.RAG_REQUIRED);
        }

        @ParameterizedTest
        @DisplayName("질문 형태 입력 시 RAG_REQUIRED 반환")
        @ValueSource(strings = {
            "무엇이 좋을까요?",
            "어떤 기술을 배워야 할까?",
            "이것은 뭔가요?",
            "언제 시작하나요?"
        })
        void classifyIntent_questionPattern(String input) {
            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.RAG_REQUIRED);
        }

        @Test
        @DisplayName("물음표가 있는 질문은 RAG_REQUIRED 반환")
        void classifyIntent_questionMark() {
            // Given
            String input = "이 기술 스택은 어떤가요?";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.RAG_REQUIRED);
        }
    }

    // ========== LLM_DIRECT 테스트 ==========

    @Nested
    @DisplayName("classifyIntent - LLM_DIRECT")
    class LlmDirect {

        @ParameterizedTest
        @DisplayName("창작/번역 요청 시 LLM_DIRECT 반환")
        @ValueSource(strings = {
            "이 문장 번역해줘",
            "코드 작성해줘",
            "이 내용 요약해줘",
            "설명해줘"
        })
        void classifyIntent_llmDirectKeywords(String input) {
            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.LLM_DIRECT);
        }

        @Test
        @DisplayName("일반 대화는 LLM_DIRECT 반환")
        void classifyIntent_generalConversation() {
            // Given
            String input = "안녕하세요";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.LLM_DIRECT);
        }

        @Test
        @DisplayName("기본값은 LLM_DIRECT")
        void classifyIntent_default() {
            // Given
            String input = "그냥 테스트 메시지";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.LLM_DIRECT);
        }
    }

    // ========== 우선순위 테스트 ==========

    @Nested
    @DisplayName("classifyIntent - 우선순위")
    class Priority {

        @Test
        @DisplayName("@agent가 최우선순위")
        void classifyIntent_agentHighestPriority() {
            // Given: @agent + 웹검색 키워드
            String input = "@agent 오늘 날씨 검색해줘";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.AGENT_COMMAND);
        }

        @Test
        @DisplayName("RAG가 웹 검색보다 우선")
        void classifyIntent_ragOverWebSearch() {
            // Given: 웹검색 키워드 + RAG 키워드
            String input = "오늘 kaggle 대회 뉴스";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then: RAG 키워드(kaggle, 대회, 뉴스)가 있으므로 RAG 우선
            assertThat(result).isEqualTo(Intent.RAG_REQUIRED);
        }
    }

    // ========== 엣지 케이스 테스트 ==========

    @Nested
    @DisplayName("classifyIntent - 엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("빈 문자열 처리")
        void classifyIntent_emptyString() {
            // Given
            String input = "";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.LLM_DIRECT);
        }

        @Test
        @DisplayName("공백만 있는 입력 처리")
        void classifyIntent_whitespaceOnly() {
            // Given
            String input = "   ";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.LLM_DIRECT);
        }

        @Test
        @DisplayName("대소문자 혼합 키워드")
        void classifyIntent_mixedCase() {
            // Given
            String input = "KAGGLE 대회 정보";

            // When
            Intent result = intentService.classifyIntent(input);

            // Then
            assertThat(result).isEqualTo(Intent.RAG_REQUIRED);
        }
    }
}
