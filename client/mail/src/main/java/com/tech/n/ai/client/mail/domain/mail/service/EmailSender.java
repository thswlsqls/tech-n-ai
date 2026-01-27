package com.tech.n.ai.client.mail.domain.mail.service;

import com.tech.n.ai.client.mail.domain.mail.dto.EmailMessage;
import com.tech.n.ai.client.mail.exception.EmailSendException;

/**
 * 이메일 발송을 추상화하는 인터페이스.
 */
public interface EmailSender {
    
    /**
     * 이메일을 동기 방식으로 발송합니다.
     * 발송 실패 시 EmailSendException을 던집니다.
     *
     * @param message 발송할 이메일 메시지
     * @throws EmailSendException 이메일 발송 실패 시
     */
    void send(EmailMessage message);
    
    /**
     * 이메일을 비동기 방식으로 발송합니다.
     * 발송 실패 시 로그만 남기고 예외를 던지지 않습니다.
     *
     * @param message 발송할 이메일 메시지
     */
    void sendAsync(EmailMessage message);
}
