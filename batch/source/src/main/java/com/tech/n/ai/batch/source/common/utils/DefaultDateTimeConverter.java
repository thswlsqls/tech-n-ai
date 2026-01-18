package com.tech.n.ai.batch.source.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;

public class DefaultDateTimeConverter {

    private DefaultDateTimeConverter() {}

    public static long convertMillisecond (LocalDateTime localDateTime) {
        if (localDateTime == null) return 0;
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static LocalDate convertDate(String date) {
        if (date == null) return null;
        return LocalDate.parse(date, DefaultDateTimeFormat.DATE_FORMAT);
    }

    public static String convertDate(LocalDate localDate) {
        if (localDate == null) return null;
        return localDate.format(DefaultDateTimeFormat.DATE_FORMAT);
    }

    public static LocalTime convertTime(String time) {
        if (time == null) return null;
        return LocalTime.parse(time, DefaultDateTimeFormat.TIME_FORMAT);
    }

    public static String convertTime(LocalTime localTime) {
        if (localTime == null) return null;
        return localTime.format(DefaultDateTimeFormat.TIME_FORMAT);
    }

    public static LocalDateTime convertDateTime(String dateTime) {
        if (dateTime == null) return null;
        return LocalDateTime.parse(dateTime, DefaultDateTimeFormat.DATE_TIME_FORMAT);
    }

    public static String convertDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.format(DefaultDateTimeFormat.DATE_TIME_FORMAT);
    }

    public static String convertDateTimeMillisecond(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.format(DefaultDateTimeFormat.DATE_TIME_MILLISECOND_FORMAT);
    }


    public static String convertNoneDashDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.format(DefaultDateTimeFormat.DATE_TIME_NONE_DASH_FORMAT);
    }

    public static LocalDateTime convertNoneDashDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return null;
        return LocalDateTime.parse(dateTime, DefaultDateTimeFormat.DATE_TIME_NONE_DASH_FORMAT);
    }

    public static String convertNoneDashDate(LocalDate localDate) {
        if (localDate == null) return null;
        return localDate.format(DefaultDateTimeFormat.DATE_NONE_DASH_FORMAT);
    }

    public static LocalDate convertNoneDashDate(String date) {
        if (date == null || date.isEmpty()) return null;
        return LocalDate.parse(date, DefaultDateTimeFormat.DATE_NONE_DASH_FORMAT);
    }

    public static YearMonth convertYearMonth(String yearMonth) {
        if (yearMonth == null) return null;
        return YearMonth.parse(yearMonth, DefaultDateTimeFormat.YEAR_MONTH_FORMAT);
    }

    public static String convertYearMonth(YearMonth yearMonth) {
        if (yearMonth == null) return null;
        return yearMonth.format(DefaultDateTimeFormat.YEAR_MONTH_FORMAT);
    }

    public static YearMonth convertNoneDashMonth(String yearMonth) {
        if (yearMonth == null) return null;
        return YearMonth.parse(yearMonth, DefaultDateTimeFormat.YEAR_MONTH_NONE_DASH_FORMAT);
    }

    public static String convertNoneDashMonth(YearMonth yearMonth) {
        if (yearMonth == null) return null;
        return yearMonth.format(DefaultDateTimeFormat.YEAR_MONTH_NONE_DASH_FORMAT);
    }

}
