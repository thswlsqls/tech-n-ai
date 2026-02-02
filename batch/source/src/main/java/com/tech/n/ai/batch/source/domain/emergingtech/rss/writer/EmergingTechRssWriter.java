package com.tech.n.ai.batch.source.domain.emergingtech.rss.writer;

import com.tech.n.ai.batch.source.domain.emergingtech.writer.AbstractEmergingTechWriter;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import org.springframework.batch.core.configuration.annotation.StepScope;

/**
 * Emerging Tech RSS → Internal API Writer
 */
@StepScope
public class EmergingTechRssWriter extends AbstractEmergingTechWriter {

    public EmergingTechRssWriter(EmergingTechInternalContract emergingTechInternalApi) {
        super(emergingTechInternalApi);
    }

    @Override
    protected String getSourceType() {
        return "RSS";
    }

    @Override
    protected String getJobName() {
        return "emerging-tech.rss.job";
    }
}
