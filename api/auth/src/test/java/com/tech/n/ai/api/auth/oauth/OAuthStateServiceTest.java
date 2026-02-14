package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthStateService 단위 테스트")
class OAuthStateServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OAuthStateService oauthStateService;

    @Nested
    @DisplayName("saveState")
    class SaveState {

        @Test
        @DisplayName("정상 저장 - Redis에 10분 TTL로 저장")
        void saveState_성공() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            oauthStateService.saveState("state-abc", "GOOGLE");

            verify(valueOperations).set("oauth:state:state-abc", "GOOGLE", Duration.ofMinutes(10));
        }
    }

    @Nested
    @DisplayName("validateAndDeleteState")
    class ValidateAndDeleteState {

        @Test
        @DisplayName("정상 검증 및 삭제")
        void validateAndDeleteState_성공() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("oauth:state:state-abc")).thenReturn("GOOGLE");
            when(redisTemplate.delete("oauth:state:state-abc")).thenReturn(true);

            assertThatCode(() -> oauthStateService.validateAndDeleteState("state-abc", "GOOGLE"))
                .doesNotThrowAnyException();
            verify(redisTemplate).delete("oauth:state:state-abc");
        }

        @Test
        @DisplayName("State 미존재 (만료 등) - UnauthorizedException")
        void validateAndDeleteState_미존재() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("oauth:state:expired-state")).thenReturn(null);

            assertThatThrownBy(() -> oauthStateService.validateAndDeleteState("expired-state", "GOOGLE"))
                .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Provider 불일치 - UnauthorizedException")
        void validateAndDeleteState_Provider_불일치() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("oauth:state:state-abc")).thenReturn("KAKAO");
            when(redisTemplate.delete("oauth:state:state-abc")).thenReturn(true);

            assertThatThrownBy(() -> oauthStateService.validateAndDeleteState("state-abc", "GOOGLE"))
                .isInstanceOf(UnauthorizedException.class);
            // Provider 불일치 시에도 state 삭제 확인
            verify(redisTemplate).delete("oauth:state:state-abc");
        }
    }
}
