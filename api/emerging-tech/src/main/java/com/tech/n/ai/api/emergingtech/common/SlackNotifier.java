package com.tech.n.ai.api.emergingtech.common;

import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Slack 알림 (기본 구현 - 실제 알림 미전송)
 * 추후 SlackClient 연동 시 구현체 추가 예정
 */
@Slf4j
@Component
public class SlackNotifier {

    /**
     * Emerging Tech 알림 전송 (기본: 로그만 출력)
     */
    public void sendEmergingTechNotification(EmergingTechDetailResponse update) {
        log.info("Slack 알림 전송 (비활성화 상태): provider={}, title={}",
            update.provider(), update.title());
    }

    /**
     * 승인 알림 전송
     */
    public void sendApprovalNotification(EmergingTechDetailResponse update) {
        log.info("Slack 승인 알림 전송 (비활성화 상태): provider={}, title={}",
            update.provider(), update.title());
    }

    /**
     * 거부 알림 전송
     */
    public void sendRejectionNotification(EmergingTechDetailResponse update) {
        log.info("Slack 거부 알림 전송 (비활성화 상태): provider={}, title={}",
            update.provider(), update.title());
    }
}
