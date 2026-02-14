package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.GitHubReleaseDto;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubContract;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * GitHubToolAdapter 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubToolAdapter 단위 테스트")
class GitHubToolAdapterTest {

    @Mock
    private GitHubContract githubContract;

    @InjectMocks
    private GitHubToolAdapter adapter;

    // ========== getReleases 테스트 ==========

    @Nested
    @DisplayName("getReleases")
    class GetReleases {

        @Test
        @DisplayName("정상 조회 - 릴리즈 목록 반환")
        void getReleases_정상조회() {
            // Given
            GitHubDto.Release release = createRelease("v1.0.0", "Release 1.0.0",
                    "Release notes", false, false);
            GitHubDto.ReleasesResponse response = new GitHubDto.ReleasesResponse(List.of(release));

            when(githubContract.getReleases(any(GitHubDto.ReleasesRequest.class)))
                    .thenReturn(response);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("openai", "openai-python");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).tagName()).isEqualTo("v1.0.0");
            assertThat(result.get(0).name()).isEqualTo("Release 1.0.0");
        }

        @Test
        @DisplayName("Draft 릴리즈 제외")
        void getReleases_Draft제외() {
            // Given
            GitHubDto.Release normalRelease = createRelease("v1.0.0", "Release",
                    "notes", false, false);
            GitHubDto.Release draftRelease = createRelease("v2.0.0-draft", "Draft",
                    "draft notes", true, false);
            GitHubDto.ReleasesResponse response = new GitHubDto.ReleasesResponse(
                    List.of(normalRelease, draftRelease));

            when(githubContract.getReleases(any())).thenReturn(response);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("openai", "repo");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).tagName()).isEqualTo("v1.0.0");
        }

        @Test
        @DisplayName("Prerelease 제외")
        void getReleases_Prerelease제외() {
            // Given
            GitHubDto.Release normalRelease = createRelease("v1.0.0", "Release",
                    "notes", false, false);
            GitHubDto.Release preRelease = createRelease("v2.0.0-beta", "Beta",
                    "beta notes", false, true);
            GitHubDto.ReleasesResponse response = new GitHubDto.ReleasesResponse(
                    List.of(normalRelease, preRelease));

            when(githubContract.getReleases(any())).thenReturn(response);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("openai", "repo");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).tagName()).isEqualTo("v1.0.0");
        }

        @Test
        @DisplayName("body 500자 초과 시 truncate")
        void getReleases_bodyTruncate() {
            // Given
            String longBody = "a".repeat(600);
            GitHubDto.Release release = createRelease("v1.0.0", "Release",
                    longBody, false, false);
            GitHubDto.ReleasesResponse response = new GitHubDto.ReleasesResponse(List.of(release));

            when(githubContract.getReleases(any())).thenReturn(response);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("owner", "repo");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).body().length()).isLessThanOrEqualTo(503); // 500 + "..."
        }

        @Test
        @DisplayName("GitHub API 호출 실패 시 빈 리스트 반환")
        void getReleases_API실패() {
            // Given
            when(githubContract.getReleases(any()))
                    .thenThrow(new RuntimeException("API 호출 실패"));

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("owner", "repo");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null 응답 시 빈 리스트 반환")
        void getReleases_null응답() {
            // Given
            when(githubContract.getReleases(any())).thenReturn(null);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("owner", "repo");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("releases null 시 빈 리스트 반환")
        void getReleases_releasesNull() {
            // Given
            GitHubDto.ReleasesResponse response = new GitHubDto.ReleasesResponse(null);
            when(githubContract.getReleases(any())).thenReturn(response);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("owner", "repo");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 릴리즈 목록 시 빈 리스트 반환")
        void getReleases_빈목록() {
            // Given
            GitHubDto.ReleasesResponse response = new GitHubDto.ReleasesResponse(List.of());
            when(githubContract.getReleases(any())).thenReturn(response);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("owner", "repo");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("여러 정상 릴리즈 조회")
        void getReleases_여러릴리즈() {
            // Given
            GitHubDto.Release r1 = createRelease("v1.0.0", "R1", "notes1", false, false);
            GitHubDto.Release r2 = createRelease("v1.1.0", "R2", "notes2", false, false);
            GitHubDto.Release r3 = createRelease("v2.0.0", "R3", "notes3", false, false);
            GitHubDto.ReleasesResponse response = new GitHubDto.ReleasesResponse(List.of(r1, r2, r3));

            when(githubContract.getReleases(any())).thenReturn(response);

            // When
            List<GitHubReleaseDto> result = adapter.getReleases("owner", "repo");

            // Then
            assertThat(result).hasSize(3);
        }
    }

    // ========== 헬퍼 메서드 ==========

    private GitHubDto.Release createRelease(String tagName, String name, String body,
                                            boolean draft, boolean prerelease) {
        return GitHubDto.Release.builder()
                .id(1L)
                .tagName(tagName)
                .name(name)
                .body(body)
                .htmlUrl("https://github.com/owner/repo/releases/tag/" + tagName)
                .publishedAt("2024-01-15T10:00:00Z")
                .draft(draft)
                .prerelease(prerelease)
                .build();
    }
}
