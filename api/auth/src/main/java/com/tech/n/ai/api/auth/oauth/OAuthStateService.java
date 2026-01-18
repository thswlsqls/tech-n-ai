package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthStateService {
    
    private static final String STATE_KEY_PREFIX = "oauth:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * State 파라미터 저장
     * 
     * @param state State 값 (암호학적으로 안전한 랜덤 값)
     * @param providerName Provider 이름 (예: "GOOGLE", "NAVER", "KAKAO")
     */
    public void saveState(String state, String providerName) {
        String key = STATE_KEY_PREFIX + state;
        redisTemplate.opsForValue().set(key, providerName, STATE_TTL);
        log.debug("OAuth state saved: provider={}, state={}", providerName, state);
    }
    
    /**
     * State 파라미터 검증 및 삭제
     * 
     * @param state State 값
     * @param providerName Provider 이름
     * @throws UnauthorizedException State 검증 실패 시
     */
    public void validateAndDeleteState(String state, String providerName) {
        String key = STATE_KEY_PREFIX + state;
        String storedProvider = redisTemplate.opsForValue().get(key);
        
        if (storedProvider == null) {
            log.warn("OAuth state not found: state={}", state);
            throw new UnauthorizedException("유효하지 않은 State 파라미터입니다.");
        }
        
        if (!storedProvider.equals(providerName)) {
            log.warn("OAuth state provider mismatch: expected={}, actual={}, state={}", 
                    providerName, storedProvider, state);
            redisTemplate.delete(key);
            throw new UnauthorizedException("유효하지 않은 State 파라미터입니다.");
        }
        
        redisTemplate.delete(key);
        log.debug("OAuth state validated and deleted: provider={}, state={}", providerName, state);
    }
}
