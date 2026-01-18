package com.tech.n.ai.batch.source.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateConverter {

    public static LocalDateTime dateTimeToMin(String source) {
        if (Objects.nonNull(source) && StringUtils.hasText(source)) {
            return LocalDate.parse(source).atTime(LocalTime.MIN);
        } else {
            return LocalDate.now().atTime(LocalTime.MIN);
        }
    }

    public static LocalDate stringToDate(String source) {
        if (Objects.nonNull(source) && StringUtils.hasText(source)) {
            return LocalDate.parse(source, DefaultDateTimeFormat.DATE_FORMAT);
        } else {
            return LocalDate.now();
        }
    }

    /**
     * 날짜의 시간 값을 MAX 값으로 변환
     * @param LocalDateTime source
     * @return LocalDateTime
     */
    public static LocalDateTime dateTimeToMax(LocalDateTime source) {
        if (Objects.nonNull(source) && LocalDateTime.now().isAfter(source)) {
            return source.with(LocalTime.MAX);
        } else {
            return LocalDate.now().atTime(LocalTime.MAX);
        }
    }

    /**
     * 날짜의 시간 값을 MAX 값으로 변환
     * @param String source yyyy-MM-dd
     * @return LocalDateTime
     */
    public static LocalDateTime dateTimeToMax(String source) {
        if (Objects.nonNull(source) && StringUtils.hasText(source)) {
            return LocalDate.parse(source).atTime(LocalTime.MAX);
        } else {
            return LocalDate.now().atTime(LocalTime.MAX);
        }
    }

}
