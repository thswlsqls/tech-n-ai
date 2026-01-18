package com.tech.n.ai.client.slack.domain.slack.client;

import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;

/**
 * Slack 클라이언트 인터페이스
 * Slack 메시지 전송을 위한 기본 인터페이스
 */
public interface SlackClient {
    
    /**
     * Block Kit 메시지 전송
     * 
     * @param message 전송할 메시지
     */
    void sendMessage(SlackDto.SlackMessage message);
    
    /**
     * 간단한 텍스트 메시지 전송
     * 
     * @param text 전송할 텍스트
     */
    void sendMessage(String text);
    
    /**
     * 특정 채널로 텍스트 메시지 전송
     * 
     * @param text 전송할 텍스트
     * @param channel 채널명 (예: "#general")
     */
    void sendMessage(String text, String channel);
}
