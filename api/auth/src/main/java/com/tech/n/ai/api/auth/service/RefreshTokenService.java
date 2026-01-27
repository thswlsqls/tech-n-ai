package com.tech.n.ai.api.auth.service;

import com.tech.n.ai.datasource.mariadb.entity.auth.RefreshTokenEntity;
import com.tech.n.ai.datasource.mariadb.entity.auth.UserEntity;
import com.tech.n.ai.datasource.mariadb.repository.reader.auth.RefreshTokenReaderRepository;
import com.tech.n.ai.datasource.mariadb.repository.reader.auth.UserReaderRepository;
import com.tech.n.ai.datasource.mariadb.repository.writer.auth.RefreshTokenWriterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenWriterRepository refreshTokenWriterRepository;
    private final RefreshTokenReaderRepository refreshTokenReaderRepository;
    private final UserReaderRepository userReaderRepository;
    
    @Transactional
    public RefreshTokenEntity saveRefreshToken(Long userId, String token, LocalDateTime expiresAt) {
        UserEntity user = userReaderRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.create(userId, token, expiresAt);
        refreshTokenEntity.setUser(user);
        return refreshTokenWriterRepository.save(refreshTokenEntity);
    }
    
    public Optional<RefreshTokenEntity> findRefreshToken(String token) {
        return refreshTokenReaderRepository.findByToken(token);
    }
    
    @Transactional
    public void deleteRefreshToken(RefreshTokenEntity refreshTokenEntity) {
        refreshTokenWriterRepository.delete(refreshTokenEntity);
    }
    
    public boolean validateRefreshToken(String token) {
        return findRefreshToken(token)
            .filter(entity -> !Boolean.TRUE.equals(entity.getIsDeleted()))
            .filter(entity -> entity.getExpiresAt().isAfter(LocalDateTime.now()))
            .isPresent();
    }
}
