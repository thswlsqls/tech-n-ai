package com.tech.n.ai.datasource.aurora.entity.auth;

import com.tech.n.ai.datasource.aurora.entity.BaseEntity;
import com.tech.n.ai.datasource.aurora.listener.HistoryEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@EntityListeners(HistoryEntityListener.class)
@Getter
@Setter
public class AdminEntity extends BaseEntity {

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "role", length = 50, nullable = false)
    private String role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at", precision = 6)
    private LocalDateTime lastLoginAt;
}
