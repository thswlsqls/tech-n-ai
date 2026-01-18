package com.tech.n.ai.batch.source.common.utils.gc;


import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;


/**
 * 작업 실행 시간 정보를 담는 불변 객체
 */
@Value
@Builder
public class ExecutionTimeInfo {
    /**
     * 작업 시작 시간
     */
    LocalDateTime startTime;

    /**
     * 작업 종료 시간
     */
    LocalDateTime endTime;

    /**
     * 실행 시간 (Duration)
     */
    Duration duration;

    /**
     * 밀리초 단위 실행 시간
     */
    public long getDurationInMillis() {
        return duration != null ? duration.toMillis() : 0L;
    }

    /**
     * 초 단위 실행 시간
     */
    public long getDurationInSeconds() {
        return duration != null ? duration.getSeconds() : 0L;
    }

    /**
     * 분 단위 실행 시간
     */
    public long getDurationInMinutes() {
        return duration != null ? duration.toMinutes() : 0L;
    }

    /**
     * 시간 단위 실행 시간
     */
    public long getDurationInHours() {
        return duration != null ? duration.toHours() : 0L;
    }

    /**
     * 사람이 읽기 쉬운 형식의 실행 시간 문자열
     * 예: "2h 30m 15s 123ms"
     */
    public String getFormattedDuration() {
        if (duration == null) {
            return "0ms";
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        long millis = duration.toMillis() % 1000;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || minutes > 0 || hours > 0) {
            sb.append(seconds).append("s ");
        }
        sb.append(millis).append("ms");

        return sb.toString().trim();
    }
}

