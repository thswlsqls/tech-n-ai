package com.tech.n.ai.client.scraper.util;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * 다중 날짜 포맷을 지원하는 날짜 파싱 유틸리티
 */
@Slf4j
public final class DateParsingUtils {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),    // Jan 15, 2026
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),   // January 15, 2026
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH),   // Jan 05, 2026
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH),  // January 05, 2026
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),     // 15 Jan 2026
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),    // 15 January 2026
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)      // 2026-01-15
    );

    private DateParsingUtils() {}

    public static LocalDateTime parseDateText(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return null;
        }

        String trimmed = dateText.trim();

        // ISO 8601 with timezone (e.g. 2026-01-28T21:15:36.668Z)
        LocalDateTime isoResult = parseIso8601(trimmed);
        if (isoResult != null) {
            return isoResult;
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(trimmed, formatter);
                return date.atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }

        log.debug("Failed to parse date with any known format: {}", trimmed);
        return null;
    }

    private static LocalDateTime parseIso8601(String text) {
        try {
            Instant instant = Instant.parse(text);
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // not ISO 8601 format
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // not ISO local datetime format
        }
        return null;
    }
}
