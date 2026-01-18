package com.tech.n.ai.api.chatbot.controller;

import com.tech.n.ai.api.chatbot.dto.request.ChatRequest;
import com.tech.n.ai.api.chatbot.dto.response.ChatResponse;
import com.tech.n.ai.api.chatbot.dto.response.MessageResponse;
import com.tech.n.ai.api.chatbot.dto.response.SessionResponse;
import com.tech.n.ai.api.chatbot.facade.ChatbotFacade;
import com.tech.n.ai.api.chatbot.service.ConversationMessageService;
import com.tech.n.ai.api.chatbot.service.ConversationSessionService;
import com.tech.n.ai.common.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 챗봇 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    
    private final ChatbotFacade chatbotFacade;
    private final ConversationSessionService conversationSessionService;
    private final ConversationMessageService conversationMessageService;
    
    /**
     * 챗봇 대화
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {
        // JWT에서 userId 추출 (authentication.getName()은 JWT의 subject, 즉 userId)
        Long userId = Long.parseLong(authentication.getName());
        
        ChatResponse response = chatbotFacade.chat(request, userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 세션 목록 조회
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<SessionResponse>>> getSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        Pageable pageable = PageRequest.of(
            page - 1,
            size,
            Sort.by(Sort.Direction.DESC, "lastMessageAt")
        );
        
        Page<SessionResponse> sessions = conversationSessionService.listSessions(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
    
    /**
     * 세션 상세 조회
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @PathVariable String sessionId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        SessionResponse response = conversationSessionService.getSession(sessionId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 메시지 히스토리 조회
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        // 세션 소유권 검증
        conversationSessionService.getSession(sessionId, userId);
        
        Pageable pageable = PageRequest.of(
            page - 1,
            size,
            Sort.by(Sort.Direction.ASC, "sequenceNumber")
        );
        
        Page<MessageResponse> messages = conversationMessageService.getMessages(sessionId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
    
    /**
     * 세션 삭제
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable String sessionId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        
        // 세션 소유권 검증 및 삭제
        // TODO: ConversationSessionService에 deleteSession 메서드 추가 필요
        // conversationSessionService.deleteSession(sessionId, userId);
        
        return ResponseEntity.ok(ApiResponse.success());
    }
}
