package com.tech.n.ai.api.chatbot.controller;

import com.tech.n.ai.api.chatbot.dto.request.ChatRequest;
import com.tech.n.ai.api.chatbot.dto.request.MessageListRequest;
import com.tech.n.ai.api.chatbot.dto.request.SessionListRequest;
import com.tech.n.ai.api.chatbot.dto.request.UpdateSessionTitleRequest;
import com.tech.n.ai.api.chatbot.dto.response.ChatResponse;
import com.tech.n.ai.api.chatbot.dto.response.MessageResponse;
import com.tech.n.ai.api.chatbot.dto.response.SessionResponse;
import com.tech.n.ai.api.chatbot.facade.ChatbotFacade;
import com.tech.n.ai.api.chatbot.service.ConversationMessageService;
import com.tech.n.ai.api.chatbot.service.ConversationSessionService;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ChatResponse response = chatbotFacade.chat(request, userPrincipal.userId(), userPrincipal.role());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<SessionResponse>>> getSessions(
            @Valid SessionListRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Pageable pageable = createPageable(request.page() - 1, request.size(), Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        Page<SessionResponse> sessions = conversationSessionService.listSessions(userPrincipal.userId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        SessionResponse response = conversationSessionService.getSession(sessionId, userPrincipal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<Page<MessageResponse>>> getMessages(
            @PathVariable String sessionId,
            @Valid MessageListRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        conversationSessionService.getSession(sessionId, userPrincipal.userId());
        Pageable pageable = createPageable(request.page() - 1, request.size(), Sort.by(Sort.Direction.ASC, "sequenceNumber"));
        Page<MessageResponse> messages = conversationMessageService.getMessages(sessionId, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PatchMapping("/sessions/{sessionId}/title")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSessionTitle(
            @PathVariable String sessionId,
            @Valid @RequestBody UpdateSessionTitleRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        SessionResponse response = conversationSessionService.updateSessionTitle(
            sessionId, userPrincipal.userId(), request.title());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        conversationSessionService.deleteSession(sessionId, userPrincipal.userId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Pageable createPageable(int page, int size, Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
