package com.tech.n.ai.client.rss.exception;

import com.ebson.shrimp.tm.demo.common.core.constants.ErrorCodeConstants;
import com.ebson.shrimp.tm.demo.common.core.exception.BaseException;

/**
 * RSS 피드 파싱 실패 시 발생하는 예외
 * HTTP 503, 비즈니스 코드 "5003" (ExternalApiException과 동일)
 */
public class RssParsingException extends BaseException {
    
    public RssParsingException(String message) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message
        );
    }
    
    public RssParsingException(String message, Throwable cause) {
        super(
            ErrorCodeConstants.SERVICE_UNAVAILABLE,
            ErrorCodeConstants.MESSAGE_CODE_SERVICE_UNAVAILABLE,
            message,
            cause
        );
    }
}
