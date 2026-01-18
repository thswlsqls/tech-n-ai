package com.tech.n.ai.api.auth.service;


import com.tech.n.ai.datasource.aurora.entity.auth.RefreshTokenEntity;
import com.tech.n.ai.datasource.aurora.repository.reader.auth.RefreshTokenReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.writer.auth.RefreshTokenWriterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;


/**
 * Refresh Token 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenWriterRepository refreshTokenWriterRepository;
    private final RefreshTokenReaderRepository refreshTokenReaderRepository;
    
    /**
     * Refresh Token 저장
     * 
     * @param userId 사용자 ID
     * @param token Refresh Token
     * @param expiresAt 만료 일시
     * @return 저장된 RefreshTokenEntity
     */
    @Transactional
    public RefreshTokenEntity saveRefreshToken(Long userId, String token, LocalDateTime expiresAt) {
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setUserId(userId);
        refreshTokenEntity.setToken(token);
        refreshTokenEntity.setExpiresAt(expiresAt);
        
        return refreshTokenWriterRepository.save(refreshTokenEntity);
    }
    
    /**
     * Refresh Token 조회
     * 
     * @param token Refresh Token
     * @return RefreshTokenEntity (Optional)
     */
    public Optional<RefreshTokenEntity> findRefreshToken(String token) {
        return refreshTokenReaderRepository.findByToken(token);
    }
    
    /**
     * Refresh Token 삭제 (Soft Delete)
     * 
     * @param refreshTokenEntity RefreshTokenEntity
     */
    @Transactional
    public void deleteRefreshToken(RefreshTokenEntity refreshTokenEntity) {
        refreshTokenWriterRepository.delete(refreshTokenEntity);
    }
    
    /**
     * Refresh Token 삭제 (Soft Delete) by ID
     * 
     * @param id RefreshToken ID
     */
    @Transactional
    public void deleteRefreshTokenById(Long id) {
        refreshTokenWriterRepository.deleteById(id);
    }
    
    /**
     * Refresh Token 검증
     * 
     * @param token Refresh Token
     * @return 유효한 Refresh Token이면 true
     */
    public boolean validateRefreshToken(String token) {
        Optional<RefreshTokenEntity> refreshTokenEntity = findRefreshToken(token);
        
        if (refreshTokenEntity.isEmpty()) {
            return false;
        }
        
        RefreshTokenEntity entity = refreshTokenEntity.get();
        
        // Soft Delete 확인
        if (Boolean.TRUE.equals(entity.getIsDeleted())) {
            return false;
        }
        
        // 만료 시간 확인
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return true;
    }
}
