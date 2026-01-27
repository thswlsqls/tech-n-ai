package com.tech.n.ai.api.agent.tool.validation;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * LangChain4j Tool 입력값 검증 유틸리티
 * 모든 검증 메서드는 검증 성공 시 null, 실패 시 에러 메시지 String을 반환
 */
public final class ToolInputValidator {

    private ToolInputValidator() {
        // 유틸리티 클래스
    }

    // 상수 정의
    private static final int MAX_STRING_LENGTH = 2000;
    private static final int MAX_URL_LENGTH = 2048;
    private static final Pattern GITHUB_OWNER_PATTERN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$");
    private static final Pattern GITHUB_REPO_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private static final Set<String> VALID_PROVIDERS = Set.of("OPENAI", "ANTHROPIC", "GOOGLE", "META");
    private static final Set<String> VALID_UPDATE_TYPES = Set.of("MODEL_RELEASE", "API_UPDATE", "SDK_RELEASE", "BLOG_POST");

    /**
     * 필수 입력값 검증 (null/blank 체크)
     *
     * @param value 검증할 값
     * @param fieldName 필드명 (에러 메시지용)
     * @return 에러 메시지 또는 null (검증 성공)
     */
    public static String validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return String.format("Error: %s는 필수 입력값입니다.", fieldName);
        }
        if (value.length() > MAX_STRING_LENGTH) {
            return String.format("Error: %s는 최대 %d자까지 입력 가능합니다.", fieldName, MAX_STRING_LENGTH);
        }
        return null;
    }

    /**
     * URL 형식 검증
     *
     * @param url 검증할 URL
     * @return 에러 메시지 또는 null (검증 성공)
     */
    public static String validateUrl(String url) {
        String requiredError = validateRequired(url, "URL");
        if (requiredError != null) {
            return requiredError;
        }

        if (url.length() > MAX_URL_LENGTH) {
            return String.format("Error: URL은 최대 %d자까지 입력 가능합니다.", MAX_URL_LENGTH);
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return "Error: URL은 http 또는 https 프로토콜만 지원합니다.";
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return "Error: 유효하지 않은 URL 형식입니다. 호스트가 필요합니다.";
            }
            return null;
        } catch (Exception e) {
            return "Error: 유효하지 않은 URL 형식입니다: " + e.getMessage();
        }
    }

    /**
     * AI Provider 검증 (선택적)
     * 빈 값은 허용 (전체 검색 시 사용)
     *
     * @param provider 검증할 provider 값
     * @return 에러 메시지 또는 null (검증 성공)
     */
    public static String validateProviderOptional(String provider) {
        // 빈 값은 허용 (전체 검색)
        if (provider == null || provider.isBlank()) {
            return null;
        }
        return validateEnum(provider, "provider", VALID_PROVIDERS);
    }

    /**
     * AI Provider 검증 (필수)
     * createDraftPost 등 provider가 필수인 경우 사용
     *
     * @param provider 검증할 provider 값
     * @return 에러 메시지 또는 null (검증 성공)
     */
    public static String validateProviderRequired(String provider) {
        String requiredError = validateRequired(provider, "provider");
        if (requiredError != null) {
            return requiredError;
        }
        return validateEnum(provider, "provider", VALID_PROVIDERS);
    }

    /**
     * Update Type 검증
     *
     * @param updateType 검증할 updateType 값
     * @return 에러 메시지 또는 null (검증 성공)
     */
    public static String validateUpdateType(String updateType) {
        String requiredError = validateRequired(updateType, "updateType");
        if (requiredError != null) {
            return requiredError;
        }
        return validateEnum(updateType, "updateType", VALID_UPDATE_TYPES);
    }

    /**
     * GitHub 저장소 파라미터 검증
     *
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     * @return 에러 메시지 또는 null (검증 성공)
     */
    public static String validateGitHubRepo(String owner, String repo) {
        String ownerError = validateRequired(owner, "owner");
        if (ownerError != null) {
            return ownerError;
        }

        String repoError = validateRequired(repo, "repo");
        if (repoError != null) {
            return repoError;
        }

        if (!GITHUB_OWNER_PATTERN.matcher(owner).matches()) {
            return "Error: GitHub owner 형식이 올바르지 않습니다. 영문자, 숫자, 하이픈만 사용 가능합니다: " + owner;
        }

        if (!GITHUB_REPO_PATTERN.matcher(repo).matches()) {
            return "Error: GitHub repo 형식이 올바르지 않습니다. 영문자, 숫자, 점, 밑줄, 하이픈만 사용 가능합니다: " + repo;
        }

        return null;
    }

    /**
     * Enum 값 검증 (내부 헬퍼)
     */
    private static String validateEnum(String value, String fieldName, Set<String> validValues) {
        String upperValue = value.toUpperCase();
        if (!validValues.contains(upperValue)) {
            return String.format("Error: %s는 다음 값 중 하나여야 합니다: %s (입력값: %s)",
                    fieldName, String.join(", ", validValues), value);
        }
        return null;
    }
}
