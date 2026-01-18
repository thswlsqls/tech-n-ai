package com.tech.n.ai.batch.source.common.jobparameter;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Getter
@NoArgsConstructor
public class CommonParameter {

    @Value("#{ T(com.tech.n.ai.batch.source.common.utils.DateConverter).dateTimeToMin(jobParameters[baseDate])}")
    private LocalDateTime baseDate;

}
