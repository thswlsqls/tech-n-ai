package com.tech.n.ai.datasource.aurora.repository.reader.auth;

import com.tech.n.ai.datasource.aurora.entity.auth.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RefreshTokenReaderRepository
 */
@Repository
public interface RefreshTokenReaderRepository extends JpaRepository<RefreshTokenEntity, Long> {
    
    /**
     * 토큰으로 조회
     * 
     * @param token Refresh Token
     * @return RefreshTokenEntity (Optional)
     */
    Optional<RefreshTokenEntity> findByToken(String token);
    
    /**
     * 사용자 ID로 조회
     * 
     * @param userId 사용자 ID
     * @return RefreshTokenEntity 목록
     */
    List<RefreshTokenEntity> findByUserId(Long userId);
}
