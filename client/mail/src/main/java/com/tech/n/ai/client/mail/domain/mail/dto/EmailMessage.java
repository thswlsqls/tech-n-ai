package com.tech.n.ai.client.mail.domain.mail.dto;

import lombok.Builder;

/**
 * 이메일 발송 요청 데이터를 캡슐화하는 DTO.
 */
@Builder
public record EmailMessage(
    String to,
    String subject,
    String htmlContent,
    String textContent
) {
    public EmailMessage {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("수신자 이메일은 필수입니다.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (htmlContent == null && textContent == null) {
            throw new IllegalArgumentException("HTML 또는 텍스트 본문 중 하나는 필수입니다.");
        }
    }
}
