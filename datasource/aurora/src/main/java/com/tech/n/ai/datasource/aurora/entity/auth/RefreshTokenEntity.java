package com.tech.n.ai.datasource.aurora.entity.auth;

import com.tech.n.ai.datasource.aurora.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * RefreshTokenEntity
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshTokenEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false, nullable = false)
    private Long userId;

    @Column(name = "token", length = 500, nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false, precision = 6)
    private LocalDateTime expiresAt;
}
