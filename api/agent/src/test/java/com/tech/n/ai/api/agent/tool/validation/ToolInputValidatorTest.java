package com.tech.n.ai.api.agent.tool.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolInputValidator 단위 테스트
 */
@DisplayName("ToolInputValidator 단위 테스트")
class ToolInputValidatorTest {

    // ========== validateRequired 테스트 ==========

    @Nested
    @DisplayName("validateRequired")
    class ValidateRequired {

        @Test
        @DisplayName("정상 입력값 - null 반환")
        void validateRequired_정상() {
            String result = ToolInputValidator.validateRequired("valid", "fieldName");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 입력 - 에러 메시지 반환")
        void validateRequired_null() {
            String result = ToolInputValidator.validateRequired(null, "fieldName");
            assertThat(result).contains("Error").contains("필수 입력값");
        }

        @Test
        @DisplayName("빈 문자열 - 에러 메시지 반환")
        void validateRequired_빈문자열() {
            String result = ToolInputValidator.validateRequired("", "fieldName");
            assertThat(result).contains("Error").contains("필수 입력값");
        }

        @Test
        @DisplayName("공백만 있는 문자열 - 에러 메시지 반환")
        void validateRequired_공백() {
            String result = ToolInputValidator.validateRequired("   ", "fieldName");
            assertThat(result).contains("Error").contains("필수 입력값");
        }

        @Test
        @DisplayName("최대 길이 초과 - 에러 메시지 반환")
        void validateRequired_최대길이초과() {
            String longString = "a".repeat(2001);
            String result = ToolInputValidator.validateRequired(longString, "fieldName");
            assertThat(result).contains("Error").contains("최대");
        }
    }

    // ========== validateUrl 테스트 ==========

    @Nested
    @DisplayName("validateUrl")
    class ValidateUrl {

        @Test
        @DisplayName("정상 http URL - null 반환")
        void validateUrl_http() {
            String result = ToolInputValidator.validateUrl("http://example.com");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("정상 https URL - null 반환")
        void validateUrl_https() {
            String result = ToolInputValidator.validateUrl("https://example.com/path");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null URL - 에러 메시지 반환")
        void validateUrl_null() {
            String result = ToolInputValidator.validateUrl(null);
            assertThat(result).contains("Error");
        }

        @Test
        @DisplayName("빈 URL - 에러 메시지 반환")
        void validateUrl_빈문자열() {
            String result = ToolInputValidator.validateUrl("");
            assertThat(result).contains("Error");
        }

        @Test
        @DisplayName("프로토콜 없는 URL - 에러 메시지 반환")
        void validateUrl_프로토콜없음() {
            String result = ToolInputValidator.validateUrl("example.com");
            assertThat(result).contains("Error");
        }

        @Test
        @DisplayName("잘못된 프로토콜 (ftp) - 에러 메시지 반환")
        void validateUrl_ftp() {
            String result = ToolInputValidator.validateUrl("ftp://example.com");
            assertThat(result).contains("Error").contains("http");
        }

        @Test
        @DisplayName("최대 URL 길이 초과 - 에러 메시지 반환")
        void validateUrl_최대길이초과() {
            String longUrl = "https://example.com/" + "a".repeat(2100);
            String result = ToolInputValidator.validateUrl(longUrl);
            assertThat(result).contains("Error").contains("최대");
        }
    }

    // ========== validateProviderRequired 테스트 ==========

    @Nested
    @DisplayName("validateProviderRequired")
    class ValidateProviderRequired {

        @Test
        @DisplayName("유효한 Provider (OPENAI) - null 반환")
        void validateProviderRequired_OPENAI() {
            String result = ToolInputValidator.validateProviderRequired("OPENAI");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("유효한 Provider 소문자 (anthropic) - null 반환")
        void validateProviderRequired_소문자() {
            String result = ToolInputValidator.validateProviderRequired("anthropic");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null Provider - 에러 메시지 반환")
        void validateProviderRequired_null() {
            String result = ToolInputValidator.validateProviderRequired(null);
            assertThat(result).contains("Error");
        }

        @Test
        @DisplayName("빈 Provider - 에러 메시지 반환")
        void validateProviderRequired_빈문자열() {
            String result = ToolInputValidator.validateProviderRequired("");
            assertThat(result).contains("Error");
        }

        @Test
        @DisplayName("잘못된 Provider - 에러 메시지 반환")
        void validateProviderRequired_잘못된값() {
            String result = ToolInputValidator.validateProviderRequired("INVALID");
            assertThat(result).contains("Error").contains("다음 값 중 하나");
        }
    }

    // ========== validateProviderOptional 테스트 ==========

    @Nested
    @DisplayName("validateProviderOptional")
    class ValidateProviderOptional {

        @Test
        @DisplayName("빈 Provider - null 반환 (선택적)")
        void validateProviderOptional_빈값() {
            assertThat(ToolInputValidator.validateProviderOptional(null)).isNull();
            assertThat(ToolInputValidator.validateProviderOptional("")).isNull();
            assertThat(ToolInputValidator.validateProviderOptional("   ")).isNull();
        }

        @Test
        @DisplayName("유효한 Provider - null 반환")
        void validateProviderOptional_유효값() {
            String result = ToolInputValidator.validateProviderOptional("GOOGLE");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("잘못된 Provider - 에러 메시지 반환")
        void validateProviderOptional_잘못된값() {
            String result = ToolInputValidator.validateProviderOptional("INVALID");
            assertThat(result).contains("Error");
        }
    }

    // ========== validateGitHubRepo 테스트 ==========

    @Nested
    @DisplayName("validateGitHubRepo")
    class ValidateGitHubRepo {

        @Test
        @DisplayName("정상 owner와 repo - null 반환")
        void validateGitHubRepo_정상() {
            String result = ToolInputValidator.validateGitHubRepo("openai", "openai-python");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("하이픈 포함 owner - null 반환")
        void validateGitHubRepo_하이픈owner() {
            String result = ToolInputValidator.validateGitHubRepo("meta-llama", "llama");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null owner - 에러 메시지 반환")
        void validateGitHubRepo_null_owner() {
            String result = ToolInputValidator.validateGitHubRepo(null, "repo");
            assertThat(result).contains("Error").contains("owner");
        }

        @Test
        @DisplayName("null repo - 에러 메시지 반환")
        void validateGitHubRepo_null_repo() {
            String result = ToolInputValidator.validateGitHubRepo("owner", null);
            assertThat(result).contains("Error").contains("repo");
        }

        @Test
        @DisplayName("잘못된 owner 형식 (공백 포함) - 에러 메시지 반환")
        void validateGitHubRepo_잘못된owner() {
            String result = ToolInputValidator.validateGitHubRepo("invalid owner", "repo");
            assertThat(result).contains("Error").contains("owner");
        }

        @Test
        @DisplayName("잘못된 repo 형식 (특수문자) - 에러 메시지 반환")
        void validateGitHubRepo_잘못된repo() {
            String result = ToolInputValidator.validateGitHubRepo("owner", "repo@invalid");
            assertThat(result).contains("Error").contains("repo");
        }
    }

    // ========== correctGitHubOwner 테스트 ==========

    @Nested
    @DisplayName("correctGitHubOwner")
    class CorrectGitHubOwner {

        @Test
        @DisplayName("anthropic → anthropics 교정")
        void correctGitHubOwner_anthropic() {
            String result = ToolInputValidator.correctGitHubOwner("anthropic");
            assertThat(result).isEqualTo("anthropics");
        }

        @Test
        @DisplayName("meta → meta-llama 교정")
        void correctGitHubOwner_meta() {
            String result = ToolInputValidator.correctGitHubOwner("meta");
            assertThat(result).isEqualTo("meta-llama");
        }

        @Test
        @DisplayName("facebook → facebookresearch 교정")
        void correctGitHubOwner_facebook() {
            String result = ToolInputValidator.correctGitHubOwner("facebook");
            assertThat(result).isEqualTo("facebookresearch");
        }

        @Test
        @DisplayName("xai → xai-org 교정")
        void correctGitHubOwner_xai() {
            String result = ToolInputValidator.correctGitHubOwner("xai");
            assertThat(result).isEqualTo("xai-org");
        }

        @Test
        @DisplayName("대문자 입력도 교정 (ANTHROPIC → anthropics)")
        void correctGitHubOwner_대문자() {
            String result = ToolInputValidator.correctGitHubOwner("ANTHROPIC");
            assertThat(result).isEqualTo("anthropics");
        }

        @Test
        @DisplayName("교정 불필요한 이름 - 원본 반환")
        void correctGitHubOwner_교정불필요() {
            String result = ToolInputValidator.correctGitHubOwner("openai");
            assertThat(result).isEqualTo("openai");
        }

        @Test
        @DisplayName("null 입력 - null 반환")
        void correctGitHubOwner_null() {
            String result = ToolInputValidator.correctGitHubOwner(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열 - 원본 반환")
        void correctGitHubOwner_빈문자열() {
            String result = ToolInputValidator.correctGitHubOwner("");
            assertThat(result).isEmpty();
        }
    }

    // ========== validateGroupByField 테스트 ==========

    @Nested
    @DisplayName("validateGroupByField")
    class ValidateGroupByField {

        @Test
        @DisplayName("유효한 필드 (provider) - null 반환")
        void validateGroupByField_provider() {
            String result = ToolInputValidator.validateGroupByField("provider");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("유효한 필드 (source_type) - null 반환")
        void validateGroupByField_source_type() {
            String result = ToolInputValidator.validateGroupByField("source_type");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("대문자 입력 (PROVIDER) - null 반환")
        void validateGroupByField_대문자() {
            String result = ToolInputValidator.validateGroupByField("PROVIDER");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 입력 - 에러 메시지 반환")
        void validateGroupByField_null() {
            String result = ToolInputValidator.validateGroupByField(null);
            assertThat(result).contains("Error");
        }

        @Test
        @DisplayName("잘못된 필드 - 에러 메시지 반환")
        void validateGroupByField_잘못된값() {
            String result = ToolInputValidator.validateGroupByField("invalid");
            assertThat(result).contains("Error").contains("다음 값 중 하나");
        }
    }

    // ========== resolveGroupByField 테스트 ==========

    @Nested
    @DisplayName("resolveGroupByField")
    class ResolveGroupByField {

        @Test
        @DisplayName("provider 매핑 성공")
        void resolveGroupByField_provider() {
            String result = ToolInputValidator.resolveGroupByField("provider");
            assertThat(result).isEqualTo("provider");
        }

        @Test
        @DisplayName("대소문자 변환 후 매핑")
        void resolveGroupByField_대문자() {
            String result = ToolInputValidator.resolveGroupByField("UPDATE_TYPE");
            assertThat(result).isEqualTo("update_type");
        }

        @Test
        @DisplayName("null 입력 - null 반환")
        void resolveGroupByField_null() {
            String result = ToolInputValidator.resolveGroupByField(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열 - null 반환")
        void resolveGroupByField_빈문자열() {
            String result = ToolInputValidator.resolveGroupByField("");
            assertThat(result).isNull();
        }
    }

    // ========== validateDateOptional 테스트 ==========

    @Nested
    @DisplayName("validateDateOptional")
    class ValidateDateOptional {

        @Test
        @DisplayName("정상 날짜 (YYYY-MM-DD) - null 반환")
        void validateDateOptional_정상() {
            String result = ToolInputValidator.validateDateOptional("2024-01-15", "startDate");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 날짜 - null 반환 (선택적)")
        void validateDateOptional_빈값() {
            assertThat(ToolInputValidator.validateDateOptional(null, "date")).isNull();
            assertThat(ToolInputValidator.validateDateOptional("", "date")).isNull();
            assertThat(ToolInputValidator.validateDateOptional("  ", "date")).isNull();
        }

        @Test
        @DisplayName("잘못된 형식 - 에러 메시지 반환")
        void validateDateOptional_잘못된형식() {
            String result = ToolInputValidator.validateDateOptional("01-15-2024", "date");
            assertThat(result).contains("Error").contains("YYYY-MM-DD");
        }

        @Test
        @DisplayName("유효하지 않은 날짜 (2월 30일) - 에러 메시지 반환")
        void validateDateOptional_유효하지않은날짜() {
            String result = ToolInputValidator.validateDateOptional("2024-02-30", "date");
            assertThat(result).contains("Error").contains("유효한 날짜");
        }
    }

    // ========== validateObjectId 테스트 ==========

    @Nested
    @DisplayName("validateObjectId")
    class ValidateObjectId {

        @Test
        @DisplayName("정상 ObjectId (24자리) - null 반환")
        void validateObjectId_정상() {
            String result = ToolInputValidator.validateObjectId("507f1f77bcf86cd799439011");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null 입력 - 에러 메시지 반환")
        void validateObjectId_null() {
            String result = ToolInputValidator.validateObjectId(null);
            assertThat(result).contains("Error");
        }

        @Test
        @DisplayName("길이 부족 - 에러 메시지 반환")
        void validateObjectId_길이부족() {
            String result = ToolInputValidator.validateObjectId("507f1f77bcf86cd7");
            assertThat(result).contains("Error").contains("24자리");
        }

        @Test
        @DisplayName("16진수 아님 - 에러 메시지 반환")
        void validateObjectId_16진수아님() {
            String result = ToolInputValidator.validateObjectId("507f1f77bcf86cd79943901g");
            assertThat(result).contains("Error").contains("16진수");
        }
    }

    // ========== normalizePage / normalizeSize 테스트 ==========

    @Nested
    @DisplayName("normalizePage and normalizeSize")
    class NormalizeHelpers {

        @Test
        @DisplayName("page 정규화: 0 → 1")
        void normalizePage_0() {
            assertThat(ToolInputValidator.normalizePage(0)).isEqualTo(1);
        }

        @Test
        @DisplayName("page 정규화: 음수 → 1")
        void normalizePage_음수() {
            assertThat(ToolInputValidator.normalizePage(-5)).isEqualTo(1);
        }

        @Test
        @DisplayName("page 정규화: 정상값 유지")
        void normalizePage_정상() {
            assertThat(ToolInputValidator.normalizePage(3)).isEqualTo(3);
        }

        @Test
        @DisplayName("size 정규화: 0 → 20")
        void normalizeSize_0() {
            assertThat(ToolInputValidator.normalizeSize(0)).isEqualTo(20);
        }

        @Test
        @DisplayName("size 정규화: 음수 → 20")
        void normalizeSize_음수() {
            assertThat(ToolInputValidator.normalizeSize(-10)).isEqualTo(20);
        }

        @Test
        @DisplayName("size 정규화: 150 → 100")
        void normalizeSize_초과() {
            assertThat(ToolInputValidator.normalizeSize(150)).isEqualTo(100);
        }

        @Test
        @DisplayName("size 정규화: 정상값 유지")
        void normalizeSize_정상() {
            assertThat(ToolInputValidator.normalizeSize(50)).isEqualTo(50);
        }
    }

    // ========== validateUpdateTypeOptional 테스트 ==========

    @Nested
    @DisplayName("validateUpdateTypeOptional")
    class ValidateUpdateTypeOptional {

        @Test
        @DisplayName("유효한 UpdateType - null 반환")
        void validateUpdateTypeOptional_유효값() {
            String result = ToolInputValidator.validateUpdateTypeOptional("MODEL_RELEASE");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 값 - null 반환 (선택적)")
        void validateUpdateTypeOptional_빈값() {
            assertThat(ToolInputValidator.validateUpdateTypeOptional(null)).isNull();
            assertThat(ToolInputValidator.validateUpdateTypeOptional("")).isNull();
        }

        @Test
        @DisplayName("잘못된 UpdateType - 에러 메시지 반환")
        void validateUpdateTypeOptional_잘못된값() {
            String result = ToolInputValidator.validateUpdateTypeOptional("INVALID");
            assertThat(result).contains("Error");
        }
    }

    // ========== validateSourceTypeOptional 테스트 ==========

    @Nested
    @DisplayName("validateSourceTypeOptional")
    class ValidateSourceTypeOptional {

        @Test
        @DisplayName("유효한 SourceType - null 반환")
        void validateSourceTypeOptional_유효값() {
            String result = ToolInputValidator.validateSourceTypeOptional("GITHUB_RELEASE");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 값 - null 반환 (선택적)")
        void validateSourceTypeOptional_빈값() {
            assertThat(ToolInputValidator.validateSourceTypeOptional(null)).isNull();
            assertThat(ToolInputValidator.validateSourceTypeOptional("")).isNull();
        }

        @Test
        @DisplayName("잘못된 SourceType - 에러 메시지 반환")
        void validateSourceTypeOptional_잘못된값() {
            String result = ToolInputValidator.validateSourceTypeOptional("INVALID");
            assertThat(result).contains("Error");
        }
    }

    // ========== validateStatusOptional 테스트 ==========

    @Nested
    @DisplayName("validateStatusOptional")
    class ValidateStatusOptional {

        @Test
        @DisplayName("유효한 Status - null 반환")
        void validateStatusOptional_유효값() {
            String result = ToolInputValidator.validateStatusOptional("PUBLISHED");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 값 - null 반환 (선택적)")
        void validateStatusOptional_빈값() {
            assertThat(ToolInputValidator.validateStatusOptional(null)).isNull();
            assertThat(ToolInputValidator.validateStatusOptional("")).isNull();
        }

        @Test
        @DisplayName("잘못된 Status - 에러 메시지 반환")
        void validateStatusOptional_잘못된값() {
            String result = ToolInputValidator.validateStatusOptional("INVALID");
            assertThat(result).contains("Error");
        }
    }
}
