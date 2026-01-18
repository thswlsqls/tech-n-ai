package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.chain.AnswerGenerationChain;
import com.tech.n.ai.api.chatbot.chain.InputInterpretationChain;
import com.tech.n.ai.api.chatbot.chain.ResultRefinementChain;
import com.tech.n.ai.api.chatbot.converter.MessageFormatConverter;
import com.tech.n.ai.api.chatbot.dto.request.ChatRequest;
import com.tech.n.ai.api.chatbot.dto.response.ChatResponse;
import com.tech.n.ai.api.chatbot.dto.response.SourceResponse;
import com.tech.n.ai.api.chatbot.memory.ConversationChatMemoryProvider;
import com.tech.n.ai.api.chatbot.service.dto.Intent;
import com.tech.n.ai.api.chatbot.service.dto.RefinedResult;
import com.tech.n.ai.api.chatbot.service.dto.SearchOptions;
import com.tech.n.ai.api.chatbot.service.dto.SearchQuery;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 챗봇 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {
    
    private final ConversationSessionService sessionService;
    private final ConversationMessageService messageService;
    private final ConversationChatMemoryProvider memoryProvider;
    
    @Qualifier("openAiMessageConverter")  // 기본 Provider: OpenAI
    private final MessageFormatConverter messageConverter;
    
    private final LLMService llmService;
    private final TokenService tokenService;
    private final IntentClassificationService intentService;
    private final InputInterpretationChain inputChain;
    private final VectorSearchService vectorSearchService;
    private final ResultRefinementChain refinementChain;
    private final AnswerGenerationChain answerChain;
    
    @Value("${chatbot.rag.max-search-results:5}")
    private int maxSearchResults;
    
    @Value("${chatbot.rag.min-similarity-score:0.7}")
    private double minSimilarityScore;
    
    @Override
    public ChatResponse generateResponse(ChatRequest request, Long userId) {
        // 1. 세션 확인 또는 생성
        String sessionId;
        if (request.conversationId() != null && !request.conversationId().isBlank()) {
            // 기존 세션 사용 시 소유권 검증 필수
            sessionService.getSession(request.conversationId(), userId);
            sessionId = request.conversationId();
        } else {
            // 새 세션 생성
            sessionId = sessionService.createSession(userId, null);
        }
        
        // 2. 의도 분류
        Intent intent = intentService.classifyIntent(request.message());
        
        String response;
        List<SourceResponse> sources = Collections.emptyList();
        
        if (intent == Intent.GENERAL_CONVERSATION) {
            // 일반 대화: RAG 없이 LLM 직접 호출 (ChatMemory 사용)
            ChatMemory chatMemory = memoryProvider.get(sessionId);
            
            // 기존 세션이면 히스토리 로드
            if (request.conversationId() != null && !request.conversationId().isBlank()) {
                List<ChatMessage> history = messageService.getMessagesForMemory(sessionId, null);
                history.forEach(chatMemory::add);
            }
            
            // 현재 사용자 메시지 추가
            UserMessage userMessage = UserMessage.from(request.message());
            chatMemory.add(userMessage);
            
            // 메시지 저장 (사용자 메시지)
            messageService.saveMessage(sessionId, "USER", request.message(), 
                tokenService.estimateTokens(request.message()));
            
            // LLM 호출
            List<ChatMessage> messages = chatMemory.messages();
            Object providerFormat = messageConverter.convertToProviderFormat(messages, null);
            response = llmService.generate(providerFormat.toString());
            
            // LLM 응답을 ChatMemory에 추가
            AiMessage aiMessage = AiMessage.from(response);
            chatMemory.add(aiMessage);
            
            // 메시지 저장 (LLM 응답)
            messageService.saveMessage(sessionId, "ASSISTANT", response,
                tokenService.estimateTokens(response));
        } else {
            // RAG 파이프라인
            // 2-1. 입력 해석
            SearchQuery searchQuery = inputChain.interpret(request.message());
            
            // 2-2. 검색 옵션 구성
            SearchOptions searchOptions = SearchOptions.builder()
                .includeContests(searchQuery.context().includesContests())
                .includeNews(searchQuery.context().includesNews())
                .includeArchives(searchQuery.context().includesArchives())
                .maxResults(maxSearchResults)
                .minSimilarityScore(minSimilarityScore)
                .build();
            
            // 2-3. 벡터 검색 (userId는 JWT에서 추출한 값 사용)
            List<com.tech.n.ai.api.chatbot.service.dto.SearchResult> searchResults = 
                vectorSearchService.search(searchQuery.query(), userId, searchOptions);
            
            // 2-4. 결과 정제
            List<RefinedResult> refinedResults = refinementChain.refine(searchResults);
            
            // 2-5. 답변 생성
            response = answerChain.generate(request.message(), refinedResults);
            
            // 2-6. 소스 정보 구성
            sources = refinedResults.stream()
                .map(r -> SourceResponse.builder()
                    .documentId(r.documentId())
                    .collectionType(r.collectionType())
                    .score(r.score())
                    .build())
                .collect(Collectors.toList());
            
            // 2-7. ChatMemory에 메시지 추가 및 저장
            ChatMemory chatMemory = memoryProvider.get(sessionId);
            
            // 기존 세션이면 히스토리 로드
            if (request.conversationId() != null && !request.conversationId().isBlank()) {
                List<ChatMessage> history = messageService.getMessagesForMemory(sessionId, null);
                history.forEach(chatMemory::add);
            }
            
            // 현재 사용자 메시지 추가
            UserMessage userMessage = UserMessage.from(request.message());
            chatMemory.add(userMessage);
            
            // 메시지 저장 (사용자 메시지)
            messageService.saveMessage(sessionId, "USER", request.message(), 
                tokenService.estimateTokens(request.message()));
            
            // LLM 응답을 ChatMemory에 추가
            AiMessage aiMessage = AiMessage.from(response);
            chatMemory.add(aiMessage);
            
            // 메시지 저장 (LLM 응답)
            messageService.saveMessage(sessionId, "ASSISTANT", response,
                tokenService.estimateTokens(response));
        }
        
        // 3. 세션 업데이트 (lastMessageAt)
        sessionService.updateLastMessageAt(sessionId);
        
        // 4. 토큰 사용량 추적
        int inputTokens = tokenService.estimateTokens(request.message());
        int outputTokens = tokenService.estimateTokens(response);
        tokenService.trackUsage(sessionId, userId.toString(), inputTokens, outputTokens);
        
        return ChatResponse.builder()
            .response(response)
            .conversationId(sessionId)
            .sources(sources)
            .build();
    }
}
