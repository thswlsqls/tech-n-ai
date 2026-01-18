package com.tech.n.ai.api.auth.service;


import com.tech.n.ai.api.auth.config.OAuthProperties;
import com.tech.n.ai.api.auth.dto.*;
import com.tech.n.ai.api.auth.oauth.OAuthProvider;
import com.tech.n.ai.api.auth.oauth.OAuthProviderFactory;
import com.tech.n.ai.api.auth.oauth.OAuthStateService;

import com.tech.n.ai.common.exception.exception.ConflictException;
import com.tech.n.ai.common.exception.exception.ResourceNotFoundException;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.common.kafka.event.UserCreatedEvent;
import com.tech.n.ai.common.kafka.event.UserUpdatedEvent;
import com.tech.n.ai.common.kafka.publisher.EventPublisher;
import com.tech.n.ai.common.security.jwt.JwtTokenPayload;
import com.tech.n.ai.common.security.jwt.JwtTokenProvider;

import com.tech.n.ai.datasource.aurora.entity.auth.EmailVerificationEntity;
import com.tech.n.ai.datasource.aurora.entity.auth.ProviderEntity;
import com.tech.n.ai.datasource.aurora.entity.auth.RefreshTokenEntity;
import com.tech.n.ai.datasource.aurora.entity.auth.UserEntity;
import com.tech.n.ai.datasource.aurora.repository.reader.auth.EmailVerificationReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.reader.auth.ProviderReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.reader.auth.UserReaderRepository;
import com.tech.n.ai.datasource.aurora.repository.writer.auth.EmailVerificationWriterRepository;
import com.tech.n.ai.datasource.aurora.repository.writer.auth.UserWriterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private static final String EMAIL_VERIFICATION_TYPE = "EMAIL_VERIFICATION";
    private static final String PASSWORD_RESET_TYPE = "PASSWORD_RESET";
    private static final String USER_ROLE = "USER";
    private static final String KAFKA_TOPIC_USER_EVENTS = "user-events";
    
    private final UserWriterRepository userWriterRepository;
    private final UserReaderRepository userReaderRepository;
    private final EmailVerificationWriterRepository emailVerificationWriterRepository;
    private final EmailVerificationReaderRepository emailVerificationReaderRepository;
    private final ProviderReaderRepository providerReaderRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    
    private final OAuthProviderFactory oauthProviderFactory;
    private final OAuthStateService oauthStateService;
    private final OAuthProperties oauthProperties;
    
    /**
     * 회원가입
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 검증
        if (userReaderRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
        
        // 사용자명 중복 검증 (실제로는 findByUsername 메서드가 필요하지만, 현재는 findAll 사용)
        // TODO: UserReaderRepository에 findByUsername 메서드 추가 필요
        boolean usernameExists = userReaderRepository.findAll().stream()
                .anyMatch(user -> request.username().equals(user.getUsername()) && !Boolean.TRUE.equals(user.getIsDeleted()));
        if (usernameExists) {
            throw new ConflictException("이미 사용 중인 사용자명입니다.");
        }
        
        // 비밀번호 해시 생성
        String hashedPassword = passwordEncoder.encode(request.password());
        
        // User 엔티티 생성
        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(hashedPassword);
        user.setIsEmailVerified(false);
        user = userWriterRepository.save(user);
        
        // EmailVerification 엔티티 생성
        String verificationToken = generateSecureToken();
        EmailVerificationEntity emailVerification = new EmailVerificationEntity();
        emailVerification.setEmail(request.email());
        emailVerification.setToken(verificationToken);
        emailVerification.setType(EMAIL_VERIFICATION_TYPE);
        emailVerification.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationWriterRepository.save(emailVerification);
        
        // Kafka 이벤트 발행
        UserCreatedEvent.UserCreatedPayload payload = new UserCreatedEvent.UserCreatedPayload(
            String.valueOf(user.getId()),
            String.valueOf(user.getId()),
            user.getUsername(),
            user.getEmail(),
            null
        );
        UserCreatedEvent event = new UserCreatedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_USER_EVENTS, event, String.valueOf(user.getId()));
        
        return new AuthResponse(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            "회원가입이 완료되었습니다. 이메일 인증을 완료해주세요."
        );
    }
    
    /**
     * 로그인
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 이메일로 User 조회
        UserEntity user = userReaderRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));
        
        // Soft Delete 확인
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        
        // 비밀번호 검증
        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        
        // 이메일 인증 여부 확인
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new UnauthorizedException("이메일 인증을 완료해주세요.");
        }
        
        // JWT 토큰 생성
        JwtTokenPayload payload = new JwtTokenPayload(
            String.valueOf(user.getId()),
            user.getEmail(),
            USER_ROLE
        );
        String accessToken = jwtTokenProvider.generateAccessToken(payload);
        String refreshToken = jwtTokenProvider.generateRefreshToken(payload);
        
        // RefreshToken 저장
        refreshTokenService.saveRefreshToken(
            user.getId(),
            refreshToken,
            jwtTokenProvider.getRefreshTokenExpiresAt()
        );
        
        // User 엔티티 업데이트 (last_login_at)
        user.setLastLoginAt(LocalDateTime.now());
        userWriterRepository.save(user);
        
        return new TokenResponse(
            accessToken,
            refreshToken,
            "Bearer",
            3600L,
            604800L
        );
    }
    
    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String userId, String refreshToken) {
        // RefreshToken 조회
        Optional<RefreshTokenEntity> refreshTokenEntity = refreshTokenService.findRefreshToken(refreshToken);
        
        if (refreshTokenEntity.isEmpty()) {
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }
        
        RefreshTokenEntity entity = refreshTokenEntity.get();
        
        // userId 일치 여부 검증
        if (!entity.getUserId().toString().equals(userId)) {
            throw new UnauthorizedException("Refresh Token의 사용자 ID가 일치하지 않습니다.");
        }
        
        // RefreshToken Soft Delete
        refreshTokenService.deleteRefreshToken(entity);
    }
    
    /**
     * 토큰 갱신
     */
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }
        
        // RefreshToken 조회
        Optional<RefreshTokenEntity> refreshTokenEntity = refreshTokenService.findRefreshToken(request.refreshToken());
        
        if (refreshTokenEntity.isEmpty() || !refreshTokenService.validateRefreshToken(request.refreshToken())) {
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }
        
        RefreshTokenEntity entity = refreshTokenEntity.get();
        
        // 만료 시간 확인
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("만료된 Refresh Token입니다.");
        }
        
        // 페이로드 추출
        JwtTokenPayload payload = jwtTokenProvider.getPayloadFromToken(request.refreshToken());
        
        // 기존 RefreshToken Soft Delete
        refreshTokenService.deleteRefreshToken(entity);
        
        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(payload);
        
        // 새로운 Refresh Token 생성 및 저장
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(payload);
        refreshTokenService.saveRefreshToken(
            Long.parseLong(payload.userId()),
            newRefreshToken,
            jwtTokenProvider.getRefreshTokenExpiresAt()
        );
        
        return new TokenResponse(
            newAccessToken,
            newRefreshToken,
            "Bearer",
            3600L,
            604800L
        );
    }
    
    /**
     * 이메일 인증
     */
    @Transactional
    public void verifyEmail(String token) {
        // EmailVerification 조회
        EmailVerificationEntity emailVerification = emailVerificationReaderRepository
                .findByTokenAndType(token, EMAIL_VERIFICATION_TYPE)
                .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 인증 토큰입니다."));
        
        // Soft Delete 확인
        if (Boolean.TRUE.equals(emailVerification.getIsDeleted())) {
            throw new ResourceNotFoundException("유효하지 않은 인증 토큰입니다.");
        }
        
        // 만료 시간 확인
        if (emailVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("만료된 인증 토큰입니다.");
        }
        
        // 중복 인증 방지
        if (emailVerification.getVerifiedAt() != null) {
            throw new ConflictException("이미 인증이 완료된 토큰입니다.");
        }
        
        // EmailVerification 업데이트
        emailVerification.setVerifiedAt(LocalDateTime.now());
        emailVerificationWriterRepository.save(emailVerification);
        
        // User 엔티티 업데이트
        UserEntity user = userReaderRepository.findByEmail(emailVerification.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        user.setIsEmailVerified(true);
        userWriterRepository.save(user);
        
        // Kafka 이벤트 발행
        Map<String, Object> updatedFields = new HashMap<>();
        updatedFields.put("isEmailVerified", true);
        UserUpdatedEvent.UserUpdatedPayload payload = new UserUpdatedEvent.UserUpdatedPayload(
            String.valueOf(user.getId()),
            String.valueOf(user.getId()),
            updatedFields
        );
        UserUpdatedEvent event = new UserUpdatedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_USER_EVENTS, event, String.valueOf(user.getId()));
    }
    
    /**
     * 비밀번호 재설정 요청
     */
    @Transactional
    public void requestPasswordReset(ResetPasswordRequest request) {
        // 이메일로 User 조회
        Optional<UserEntity> userOpt = userReaderRepository.findByEmail(request.email());
        
        // 존재하지 않는 이메일인 경우에도 성공 응답 반환 (보안상 일반적)
        if (userOpt.isEmpty() || Boolean.TRUE.equals(userOpt.get().getIsDeleted())) {
            return;
        }
        
        UserEntity user = userOpt.get();
        
        // 기존 PASSWORD_RESET 타입 토큰 무효화 (Soft Delete)
        emailVerificationReaderRepository.findByEmailAndType(request.email(), PASSWORD_RESET_TYPE)
                .forEach(existing -> {
                    existing.setIsDeleted(true);
                    existing.setDeletedAt(LocalDateTime.now());
                    emailVerificationWriterRepository.save(existing);
                });
        
        // 비밀번호 재설정 토큰 생성
        String resetToken = generateSecureToken();
        EmailVerificationEntity emailVerification = new EmailVerificationEntity();
        emailVerification.setEmail(request.email());
        emailVerification.setToken(resetToken);
        emailVerification.setType(PASSWORD_RESET_TYPE);
        emailVerification.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailVerificationWriterRepository.save(emailVerification);
        
        // 이메일 발송 (비동기 처리, 여기서는 로그만 출력)
        log.info("Password reset email sent to: {}", request.email());
    }
    
    /**
     * 비밀번호 재설정 확인
     */
    @Transactional
    public void confirmPasswordReset(ResetPasswordConfirmRequest request) {
        // EmailVerification 조회
        EmailVerificationEntity emailVerification = emailVerificationReaderRepository
                .findByTokenAndType(request.token(), PASSWORD_RESET_TYPE)
                .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 토큰입니다."));
        
        // Soft Delete 확인
        if (Boolean.TRUE.equals(emailVerification.getIsDeleted())) {
            throw new ResourceNotFoundException("유효하지 않은 토큰입니다.");
        }
        
        // 만료 시간 확인
        if (emailVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("만료된 토큰입니다.");
        }
        
        // 토큰 재사용 방지
        if (emailVerification.getVerifiedAt() != null) {
            throw new ConflictException("이미 사용된 토큰입니다.");
        }
        
        // User 조회
        UserEntity user = userReaderRepository.findByEmail(emailVerification.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        // 비밀번호 재사용 방지 확인
        if (user.getPassword() != null && passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ConflictException("이전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
        }
        
        // 새로운 비밀번호 해시 생성
        String hashedPassword = passwordEncoder.encode(request.newPassword());
        
        // User 엔티티 업데이트
        user.setPassword(hashedPassword);
        userWriterRepository.save(user);
        
        // EmailVerification 업데이트
        emailVerification.setVerifiedAt(LocalDateTime.now());
        emailVerificationWriterRepository.save(emailVerification);
        
        // Kafka 이벤트 발행
        Map<String, Object> updatedFields = new HashMap<>();
        updatedFields.put("password", "***");
        UserUpdatedEvent.UserUpdatedPayload payload = new UserUpdatedEvent.UserUpdatedPayload(
            String.valueOf(user.getId()),
            String.valueOf(user.getId()),
            updatedFields
        );
        UserUpdatedEvent event = new UserUpdatedEvent(payload);
        eventPublisher.publish(KAFKA_TOPIC_USER_EVENTS, event, String.valueOf(user.getId()));
    }
    
    /**
     * OAuth 로그인 시작
     */
    public String startOAuthLogin(String providerName) {
        // Provider 조회
        ProviderEntity provider = providerReaderRepository.findByName(providerName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("지원하지 않는 OAuth 제공자입니다."));
        
        // 활성화 여부 확인
        if (!Boolean.TRUE.equals(provider.getIsEnabled())) {
            throw new UnauthorizedException("비활성화된 OAuth 제공자입니다.");
        }
        
        // CSRF 방지를 위한 state 파라미터 생성
        String state = generateSecureToken();
        
        // State 저장 (Redis)
        oauthStateService.saveState(state, providerName.toUpperCase());
        
        // OAuthProvider 조회
        OAuthProvider oauthProvider = oauthProviderFactory.getProvider(providerName);
        
        // Redirect URI 가져오기
        String redirectUri = getRedirectUri(providerName.toUpperCase());
        
        // OAuth 인증 URL 생성
        String authUrl = oauthProvider.generateAuthorizationUrl(
            provider.getClientId(),
            redirectUri,
            state
        );
        
        return authUrl;
    }
    
    /**
     * OAuth 로그인 콜백
     */
    @Transactional
    public TokenResponse handleOAuthCallback(String providerName, String code, String state) {
        // Provider 조회
        ProviderEntity provider = providerReaderRepository.findByName(providerName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("지원하지 않는 OAuth 제공자입니다."));
        
        // 활성화 여부 확인
        if (!Boolean.TRUE.equals(provider.getIsEnabled())) {
            throw new UnauthorizedException("비활성화된 OAuth 제공자입니다.");
        }
        
        // State 검증 및 삭제
        oauthStateService.validateAndDeleteState(state, providerName.toUpperCase());
        
        // OAuthProvider 조회
        OAuthProvider oauthProvider = oauthProviderFactory.getProvider(providerName);
        
        // Redirect URI 가져오기
        String redirectUri = getRedirectUri(providerName.toUpperCase());
        
        // OAuth 인증 코드로 Access Token 교환
        String oauthAccessToken = oauthProvider.exchangeAccessToken(
            code,
            provider.getClientId(),
            provider.getClientSecret(),
            redirectUri
        );
        
        // OAuth 제공자 API로 사용자 정보 조회
        OAuthUserInfo oauthUserInfo = oauthProvider.getUserInfo(oauthAccessToken);
        
        if (oauthUserInfo == null) {
            throw new UnauthorizedException("사용자 정보를 가져올 수 없습니다.");
        }
        
        // User 엔티티 조회/생성
        Optional<UserEntity> userOpt = userReaderRepository.findByProviderIdAndProviderUserId(
                provider.getId(), oauthUserInfo.providerUserId());
        
        UserEntity user;
        boolean isNewUser = false;
        
        if (userOpt.isEmpty() || Boolean.TRUE.equals(userOpt.get().getIsDeleted())) {
            // 새 사용자 생성
            user = new UserEntity();
            user.setEmail(oauthUserInfo.email());
            user.setUsername(oauthUserInfo.username());
            user.setPassword(null);
            user.setProviderId(provider.getId());
            user.setProviderUserId(oauthUserInfo.providerUserId());
            user.setIsEmailVerified(true);
            user = userWriterRepository.save(user);
            isNewUser = true;
        } else {
            // 기존 사용자 업데이트
            user = userOpt.get();
            user.setEmail(oauthUserInfo.email());
            user.setUsername(oauthUserInfo.username());
            user = userWriterRepository.save(user);
        }
        
        // JWT 토큰 생성
        JwtTokenPayload payload = new JwtTokenPayload(
            String.valueOf(user.getId()),
            user.getEmail(),
            USER_ROLE
        );
        String accessToken = jwtTokenProvider.generateAccessToken(payload);
        String refreshToken = jwtTokenProvider.generateRefreshToken(payload);
        
        // RefreshToken 저장
        refreshTokenService.saveRefreshToken(
            user.getId(),
            refreshToken,
            jwtTokenProvider.getRefreshTokenExpiresAt()
        );
        
        // Kafka 이벤트 발행
        if (isNewUser) {
            UserCreatedEvent.UserCreatedPayload createdPayload = new UserCreatedEvent.UserCreatedPayload(
                String.valueOf(user.getId()),
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getEmail(),
                null
            );
            UserCreatedEvent event = new UserCreatedEvent(createdPayload);
            eventPublisher.publish(KAFKA_TOPIC_USER_EVENTS, event, String.valueOf(user.getId()));
        } else {
            Map<String, Object> updatedFields = new HashMap<>();
            updatedFields.put("email", user.getEmail());
            updatedFields.put("username", user.getUsername());
            UserUpdatedEvent.UserUpdatedPayload updatedPayload = new UserUpdatedEvent.UserUpdatedPayload(
                String.valueOf(user.getId()),
                String.valueOf(user.getId()),
                updatedFields
            );
            UserUpdatedEvent event = new UserUpdatedEvent(updatedPayload);
            eventPublisher.publish(KAFKA_TOPIC_USER_EVENTS, event, String.valueOf(user.getId()));
        }
        
        return new TokenResponse(
            accessToken,
            refreshToken,
            "Bearer",
            3600L,
            604800L
        );
    }
    
    /**
     * Provider 이름으로 Redirect URI 가져오기
     */
    private String getRedirectUri(String providerName) {
        return switch (providerName) {
            case "GOOGLE" -> oauthProperties.getGoogle().getRedirectUri();
            case "NAVER" -> oauthProperties.getNaver().getRedirectUri();
            case "KAKAO" -> oauthProperties.getKakao().getRedirectUri();
            default -> throw new ResourceNotFoundException("지원하지 않는 OAuth 제공자입니다: " + providerName);
        };
    }
    
    /**
     * 암호학적으로 안전한 랜덤 토큰 생성
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
