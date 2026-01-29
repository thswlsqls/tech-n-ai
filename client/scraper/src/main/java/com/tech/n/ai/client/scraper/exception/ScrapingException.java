package com.tech.n.ai.client.scraper.exception;

import com.ebson.shrimp.tm.demo.common.core.constants.ErrorCodeConstants;
import com.ebson.shrimp.tm.demo.common.core.exception.BaseException;

/**
 * 웹 스크래핑 실패 시 발생하는 예외
 * HTTP 503, 비즈니스 코드 "5003" (ExternalApiException과 동일)
 */
public class ScrapingException extends BaseException {
    
    public ScrapingException(String message) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message
        );
    }
    
    public ScrapingException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message,
            cause
        );
    }
}
