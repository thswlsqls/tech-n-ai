package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.dto.SearchContext;
import com.tech.n.ai.api.chatbot.service.dto.SearchQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InputInterpretationChain 단위 테스트
 *
 * 순수 비즈니스 로직 테스트 (외부 의존성 없음)
 */
@DisplayName("InputInterpretationChain 단위 테스트")
class InputInterpretationChainTest {

    private final InputInterpretationChain chain = new InputInterpretationChain();

    // ========== interpret 테스트 ==========

    @Nested
    @DisplayName("interpret - 쿼리 정제")
    class QueryCleaning {

        @ParameterizedTest
        @DisplayName("노이즈 패턴 제거")
        @ValueSource(strings = {
            "AI 트렌드 알려줘",
            "AI 트렌드 찾아줘",
            "AI 트렌드 검색해줘",
            "AI 트렌드 보여줘"
        })
        void interpret_removeNoisePatterns(String input) {
            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.query()).isEqualTo("AI 트렌드");
        }

        @Test
        @DisplayName("존칭 노이즈 패턴 제거")
        void interpret_removeFormalNoisePatterns() {
            // Given
            String input = "머신러닝 정보 알려주세요";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.query()).isEqualTo("머신러닝 정보");
        }

        @Test
        @DisplayName("노이즈 없는 입력은 그대로 유지")
        void interpret_noNoisePattern() {
            // Given
            String input = "AI 기술 동향";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.query()).isEqualTo("AI 기술 동향");
        }
    }

    // ========== 컨텍스트 분석 테스트 ==========

    @Nested
    @DisplayName("interpret - 컨텍스트 분석")
    class ContextAnalysis {

        @Test
        @DisplayName("기술 키워드 포함 시 emerging_techs 컬렉션 추가")
        void interpret_techKeyword() {
            // Given
            String input = "emerging tech 트렌드";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            SearchContext context = result.context();
            assertThat(context.includesEmergingTechs()).isTrue();
        }

        @Test
        @DisplayName("키워드 매칭 없어도 emerging_techs 컬렉션 기본 설정")
        void interpret_noKeywordMatchEmergingTechDefault() {
            // Given
            String input = "일반 질문입니다";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            SearchContext context = result.context();
            assertThat(context.includesEmergingTechs()).isTrue();
        }
    }

    // ========== SearchQuery 구조 테스트 ==========

    @Nested
    @DisplayName("interpret - SearchQuery 구조")
    class SearchQueryStructure {

        @Test
        @DisplayName("SearchQuery가 query와 context를 포함")
        void interpret_searchQueryStructure() {
            // Given
            String input = "AI 트렌드 알려줘";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.query()).isNotNull();
            assertThat(result.context()).isNotNull();
        }

        @Test
        @DisplayName("context의 getCollections가 비어있지 않음")
        void interpret_contextHasCollections() {
            // Given
            String input = "테스트 입력";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().getCollections()).isNotEmpty();
        }
    }

    // ========== Provider 감지 테스트 ==========

    @Nested
    @DisplayName("interpret - Provider 감지")
    class ProviderDetection {

        @ParameterizedTest
        @DisplayName("OpenAI 키워드 감지")
        @ValueSource(strings = {
            "openai 최신 업데이트",
            "OpenAI SDK 릴리즈"
        })
        void interpret_detectsOpenAI(String input) {
            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().getDetectedProviders()).containsExactly("OPENAI");
        }

        @ParameterizedTest
        @DisplayName("Anthropic/Claude 키워드 감지")
        @ValueSource(strings = {
            "anthropic 새로운 모델",
            "claude 업데이트 정보"
        })
        void interpret_detectsAnthropic(String input) {
            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().getDetectedProviders()).containsExactly("ANTHROPIC");
        }

        @ParameterizedTest
        @DisplayName("Google/Gemini 키워드 감지")
        @ValueSource(strings = {
            "google AI 업데이트",
            "gemini 모델 출시"
        })
        void interpret_detectsGoogle(String input) {
            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().getDetectedProviders()).containsExactly("GOOGLE");
        }

        @ParameterizedTest
        @DisplayName("Meta/Llama 키워드 감지")
        @ValueSource(strings = {
            "meta AI 소식",
            "llama 모델 정보"
        })
        void interpret_detectsMeta(String input) {
            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().getDetectedProviders()).containsExactly("META");
        }

        @ParameterizedTest
        @DisplayName("XAI/Grok 키워드 감지")
        @ValueSource(strings = {
            "xai 업데이트",
            "grok 최신 소식"
        })
        void interpret_detectsXAI(String input) {
            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().getDetectedProviders()).containsExactly("XAI");
        }

        @Test
        @DisplayName("Provider 키워드 없으면 빈 리스트")
        void interpret_noProvider() {
            // When
            SearchQuery result = chain.interpret("AI 기술 동향");

            // Then
            assertThat(result.context().getDetectedProviders()).isEmpty();
        }

        @Test
        @DisplayName("다중 provider 동시 감지")
        void interpret_detectsMultipleProviders() {
            // When
            SearchQuery result = chain.interpret("openai와 anthropic 최신 model release 비교");

            // Then
            assertThat(result.context().getDetectedProviders())
                .containsExactlyInAnyOrder("OPENAI", "ANTHROPIC");
        }

        @Test
        @DisplayName("중복 provider는 한 번만 추가")
        void interpret_noDuplicateProviders() {
            // When: openai가 두 번 언급
            SearchQuery result = chain.interpret("openai의 openai 모델 정보");

            // Then
            long openaiCount = result.context().getDetectedProviders().stream()
                .filter("OPENAI"::equals).count();
            assertThat(openaiCount).isEqualTo(1);
        }
    }

    // ========== UpdateType 감지 테스트 ==========

    @Nested
    @DisplayName("interpret - UpdateType 감지")
    class UpdateTypeDetection {

        @Test
        @DisplayName("SDK 키워드 → SDK_RELEASE 감지")
        void interpret_detectsSDKRelease() {
            // When
            SearchQuery result = chain.interpret("openai sdk 업데이트");

            // Then
            assertThat(result.context().getDetectedUpdateTypes()).contains("SDK_RELEASE");
        }

        @Test
        @DisplayName("모델 출시 키워드 → MODEL_RELEASE 감지")
        void interpret_detectsModelRelease_korean() {
            // When
            SearchQuery result = chain.interpret("최신 모델 출시 정보");

            // Then
            assertThat(result.context().getDetectedUpdateTypes()).contains("MODEL_RELEASE");
        }

        @Test
        @DisplayName("model release 영문 키워드 → MODEL_RELEASE 감지")
        void interpret_detectsModelRelease_english() {
            // When
            SearchQuery result = chain.interpret("openai model release");

            // Then
            assertThat(result.context().getDetectedUpdateTypes()).contains("MODEL_RELEASE");
        }

        @Test
        @DisplayName("제품 출시 키워드 → PRODUCT_LAUNCH 감지")
        void interpret_detectsProductLaunch() {
            // When
            SearchQuery result = chain.interpret("새로운 제품 출시 소식");

            // Then
            assertThat(result.context().getDetectedUpdateTypes()).contains("PRODUCT_LAUNCH");
        }

        @Test
        @DisplayName("플랫폼 업데이트 키워드 → PLATFORM_UPDATE 감지")
        void interpret_detectsPlatformUpdate() {
            // When
            SearchQuery result = chain.interpret("openai 플랫폼 업데이트");

            // Then
            assertThat(result.context().getDetectedUpdateTypes()).contains("PLATFORM_UPDATE");
        }

        @Test
        @DisplayName("블로그 포스트 키워드 → BLOG_POST 감지")
        void interpret_detectsBlogPost() {
            // When
            SearchQuery result = chain.interpret("최신 블로그 포스트");

            // Then
            assertThat(result.context().getDetectedUpdateTypes()).contains("BLOG_POST");
        }

        @Test
        @DisplayName("다중 updateType 동시 감지")
        void interpret_detectsMultipleUpdateTypes() {
            // When
            SearchQuery result = chain.interpret("openai sdk와 model release 정보");

            // Then
            assertThat(result.context().getDetectedUpdateTypes())
                .containsExactlyInAnyOrder("SDK_RELEASE", "MODEL_RELEASE");
        }

        @Test
        @DisplayName("updateType 키워드 없으면 빈 리스트")
        void interpret_noUpdateType() {
            // When
            SearchQuery result = chain.interpret("AI 기술 동향");

            // Then
            assertThat(result.context().getDetectedUpdateTypes()).isEmpty();
        }

        @Test
        @DisplayName("중복 updateType은 한 번만 추가")
        void interpret_noDuplicateUpdateTypes() {
            // When: sdk가 한 번만 매칭
            SearchQuery result = chain.interpret("openai sdk 관련 sdk 정보");

            // Then
            long sdkCount = result.context().getDetectedUpdateTypes().stream()
                .filter("SDK_RELEASE"::equals).count();
            assertThat(sdkCount).isEqualTo(1);
        }
    }

    // ========== Recency 감지 테스트 ==========

    @Nested
    @DisplayName("interpret - Recency 감지")
    class RecencyDetection {

        @ParameterizedTest
        @DisplayName("최신성 키워드 감지")
        @ValueSource(strings = {
            "최신 AI 업데이트",
            "최근 모델 출시",
            "latest openai sdk",
            "newest AI 기술",
            "recent updates",
            "새로운 모델"
        })
        void interpret_detectsRecencyKeywords(String input) {
            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().isRecencyDetected()).isTrue();
        }

        @Test
        @DisplayName("최신성 키워드 없으면 false")
        void interpret_noRecencyKeyword() {
            // When
            SearchQuery result = chain.interpret("AI 기술 개요");

            // Then
            assertThat(result.context().isRecencyDetected()).isFalse();
        }
    }

    // ========== Provider + UpdateType + Recency 복합 테스트 ==========

    @Nested
    @DisplayName("interpret - 복합 감지")
    class CombinedDetection {

        @Test
        @DisplayName("Provider + UpdateType + Recency 동시 감지")
        void interpret_detectsAllContextFields() {
            // When
            SearchQuery result = chain.interpret("최신 openai sdk 업데이트");

            // Then
            SearchContext context = result.context();
            assertThat(context.getDetectedProviders()).containsExactly("OPENAI");
            assertThat(context.getDetectedUpdateTypes()).contains("SDK_RELEASE");
            assertThat(context.isRecencyDetected()).isTrue();
        }
    }

    // ========== 엣지 케이스 ==========

    @Nested
    @DisplayName("interpret - 엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("빈 문자열 처리")
        void interpret_emptyString() {
            // Given
            String input = "";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.query()).isEmpty();
            // emerging_techs 컬렉션 기본 설정
            assertThat(result.context().includesEmergingTechs()).isTrue();
        }

        @Test
        @DisplayName("공백만 있는 입력")
        void interpret_whitespaceOnly() {
            // Given
            String input = "   ";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.query()).isEmpty();
        }

        @Test
        @DisplayName("대소문자 무시하고 키워드 인식")
        void interpret_caseInsensitive() {
            // Given
            String input = "AI TREND 분석";

            // When
            SearchQuery result = chain.interpret(input);

            // Then
            assertThat(result.context().includesEmergingTechs()).isTrue();
        }
    }
}
