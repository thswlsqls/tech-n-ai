package com.tech.n.ai.batch.source.common.incrementer;

import static java.time.format.DateTimeFormatter.ofPattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class UniqueRunIdIncrementer extends RunIdIncrementer {
    private static final String RUN_ID = "run.id";

    private String baseDate;
    private String version;

    @Override
    public JobParameters getNext(JobParameters parameters) {

        JobParameters params = (parameters == null) ? new JobParameters() : parameters;

        log.info("UniqueRunIdIncrementer^^baseDate: {}", baseDate);
        log.info("UniqueRunIdIncrementer^^version: {}", version);

        return new JobParametersBuilder()
            .addLong(RUN_ID, safeGetLong(params, RUN_ID, 0L) + 1)
            .addString("baseDate", baseDate)
            .addString("version", version)
            .toJobParameters();
    }

    public static Long safeGetLong(JobParameters params, String key, Long defaultValue) {
        Long result = params.getLong(key);
        return result != null ? result : defaultValue;
    }

    public static String formatBaseDateTime(String baseDate) {
        DateTimeFormatter dateTimeFormat = ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        if (Objects.isNull(baseDate) || baseDate.isBlank()) {
            return LocalDateTime.now().format(dateTimeFormat);
        } else {
            LocalDate date = LocalDate.parse(baseDate, ofPattern("yyyy-MM-dd"));
            return LocalDateTime.of(date, LocalTime.MAX).format(dateTimeFormat);
        }
    }
}


