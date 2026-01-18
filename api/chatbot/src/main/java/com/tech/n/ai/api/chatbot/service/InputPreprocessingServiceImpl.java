package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.common.exception.InvalidInputException;
import com.tech.n.ai.api.chatbot.service.dto.PreprocessedInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 입력 전처리 서비스 구현체
 */
@Slf4j
@Service
public class InputPreprocessingServiceImpl implements InputPreprocessingService {
    
    @Value("${chatbot.input.max-length:500}")
    private int maxLength;
    
    @Value("${chatbot.input.min-length:1}")
    private int minLength;
    
    @Override
    public PreprocessedInput preprocess(String rawInput) {
        // 1. Null 및 빈 문자열 검증
        if (rawInput == null || rawInput.isBlank()) {
            throw new InvalidInputException("입력이 비어있습니다.");
        }
        
        // 2. 길이 검증
        if (rawInput.length() > maxLength) {
            throw new InvalidInputException(
                String.format("입력 길이는 %d자를 초과할 수 없습니다.", maxLength)
            );
        }
        if (rawInput.length() < minLength) {
            throw new InvalidInputException(
                String.format("입력 길이는 최소 %d자 이상이어야 합니다.", minLength)
            );
        }
        
        // 3. 정규화
        String normalized = normalize(rawInput);
        
        // 4. 특수 문자 필터링
        String cleaned = cleanSpecialCharacters(normalized);
        
        return PreprocessedInput.builder()
            .original(rawInput)
            .normalized(normalized)
            .cleaned(cleaned)
            .length(cleaned.length())
            .build();
    }
    
    /**
     * 입력 정규화
     */
    private String normalize(String input) {
        // 1. 앞뒤 공백 제거
        String trimmed = input.trim();
        
        // 2. 연속 공백을 단일 공백으로 변환
        trimmed = trimmed.replaceAll("\\s+", " ");
        
        return trimmed;
    }
    
    /**
     * 특수 문자 필터링
     */
    private String cleanSpecialCharacters(String input) {
        // 제어 문자 제거 (탭, 개행 등은 공백으로 변환)
        String cleaned = input.replaceAll("[\\x00-\\x1F\\x7F]", " ");
        
        // 연속 공백 다시 정리
        cleaned = cleaned.replaceAll("\\s+", " ");
        
        return cleaned.trim();
    }
}
