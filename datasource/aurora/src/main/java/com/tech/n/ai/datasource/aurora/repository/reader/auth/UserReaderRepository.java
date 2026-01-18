package com.tech.n.ai.datasource.aurora.repository.reader.auth;

import com.tech.n.ai.datasource.aurora.entity.auth.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserReaderRepository
 */
@Repository
public interface UserReaderRepository extends JpaRepository<UserEntity, Long> {
    
    /**
     * 이메일로 조회
     * 
     * @param email 이메일
     * @return UserEntity (Optional)
     */
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Provider ID와 Provider User ID로 조회 (OAuth 사용자 조회)
     * 
     * @param providerId Provider ID
     * @param providerUserId Provider User ID
     * @return UserEntity (Optional)
     */
    Optional<UserEntity> findByProviderIdAndProviderUserId(Long providerId, String providerUserId);
}
