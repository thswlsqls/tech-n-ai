-- Fluent Bit PII 마스킹
-- 06-security-and-iam.md §5.5 PII 보호 정책 + 한국 통신비밀보호법·개인정보보호법 준수
-- 마스킹 대상: 이메일, 전화번호(한국 휴대전화), 신용카드, 주민등록번호 패턴, JWT
--
-- 본 스크립트는 record 의 message·log·msg 필드를 검사하고, 해당 패턴을 [REDACTED] 토큰으로 치환.
-- 처리 시간 < 1ms 목표 (간단 정규식만 사용).

local function mask_string(s)
    if type(s) ~= "string" then
        return s
    end

    -- 이메일
    s = string.gsub(s, "[%w%.%-+_]+@[%w%.%-]+%.[%a]+", "[EMAIL_REDACTED]")

    -- 한국 휴대전화 010-1234-5678, 01012345678, +82-10-1234-5678
    s = string.gsub(s, "01[016789][- ]?%d%d%d%d?[- ]?%d%d%d%d", "[PHONE_REDACTED]")
    s = string.gsub(s, "%+82[- ]?10[- ]?%d%d%d%d?[- ]?%d%d%d%d", "[PHONE_REDACTED]")

    -- 신용카드 (4-4-4-4)
    s = string.gsub(s, "(%d%d%d%d)[- ]?(%d%d%d%d)[- ]?(%d%d%d%d)[- ]?(%d%d%d%d)", "[CARD_REDACTED]")

    -- 주민등록번호 (YYMMDD-NNNNNNN)
    s = string.gsub(s, "(%d%d%d%d%d%d)%-?[1-4]%d%d%d%d%d%d", "[RRN_REDACTED]")

    -- JWT 토큰 (eyJ...로 시작)
    s = string.gsub(s, "eyJ[%w%-_=]+%.[%w%-_=]+%.[%w%-_=]+", "[JWT_REDACTED]")

    -- AWS 액세스 키 ID (AKIA + 16자)
    s = string.gsub(s, "AKIA[%u%d]%w%w%w%w%w%w%w%w%w%w%w%w%w%w%w", "[AWS_KEY_REDACTED]")

    return s
end

function mask_pii(tag, timestamp, record)
    -- 후보 키들에 대해 마스킹 적용
    for _, key in ipairs({"message", "log", "msg", "exception", "stack_trace"}) do
        if record[key] ~= nil then
            record[key] = mask_string(record[key])
        end
    end

    -- mdc 같은 nested 필드 (Spring 의 MDC)
    if type(record["mdc"]) == "table" then
        for k, v in pairs(record["mdc"]) do
            if type(v) == "string" then
                record["mdc"][k] = mask_string(v)
            end
        end
    end

    return 1, timestamp, record
end
