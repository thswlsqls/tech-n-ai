package com.tech.n.ai.api.gateway.controller;

import com.tech.n.ai.common.core.constants.ErrorCodeConstants;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.core.dto.MessageCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Circuit Breaker Fallback 컨트롤러
 *
 * 백엔드 서비스 장애 시 Circuit Breaker가 열리면 이 엔드포인트로 요청이 전달됩니다.
 * 표준 ApiResponse 형식으로 503 Service Unavailable 응답을 반환합니다.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public ResponseEntity<ApiResponse<Void>> fallback() {
        MessageCode messageCode = new MessageCode(
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            "서비스가 일시적으로 불가합니다. 잠시 후 다시 시도해주세요."
        );
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            messageCode
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
