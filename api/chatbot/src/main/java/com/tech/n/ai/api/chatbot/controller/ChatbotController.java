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

@Slf4j
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    
    private final ChatbotFacade chatbotFacade;
    private final ConversationSessionService conversationSessionService;
    private final ConversationMessageService conversationMessageService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        ChatResponse response = chatbotFacade.chat(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<SessionResponse>>> getSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        Pageable pageable = createPageable(page - 1, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<SessionResponse> sessions = conversationSessionService.listSessions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
    
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @PathVariable String sessionId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        SessionResponse response = conversationSessionService.getSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        conversationSessionService.getSession(sessionId, userId);
        Pageable pageable = createPageable(page - 1, size, Sort.by(Sort.Direction.ASC, "sequenceNumber"));
        Page<MessageResponse> messages = conversationMessageService.getMessages(sessionId, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable String sessionId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        conversationSessionService.deleteSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    private Long extractUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
    
    private Pageable createPageable(int page, int size, Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
