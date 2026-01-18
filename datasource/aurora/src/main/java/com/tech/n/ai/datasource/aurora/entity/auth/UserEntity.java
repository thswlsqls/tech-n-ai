package com.tech.n.ai.datasource.aurora.entity.auth;

import com.tech.n.ai.datasource.aurora.entity.BaseEntity;
import com.tech.n.ai.datasource.aurora.listener.HistoryEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(HistoryEntityListener.class)
@Getter
@Setter
public class UserEntity extends BaseEntity {

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private ProviderEntity provider;

    @Column(name = "provider_id", insertable = false, updatable = false)
    private Long providerId;

    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "last_login_at", precision = 6)
    private LocalDateTime lastLoginAt;
}
