package com.tech.n.ai.batch.source.domain.emergingtech.scraper.writer;

import com.tech.n.ai.batch.source.domain.emergingtech.writer.AbstractEmergingTechWriter;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import org.springframework.batch.core.configuration.annotation.StepScope;

/**
 * Emerging Tech Scraper → Internal API Writer
 */
@StepScope
public class EmergingTechScraperWriter extends AbstractEmergingTechWriter {

    public EmergingTechScraperWriter(EmergingTechInternalContract emergingTechInternalApi) {
        super(emergingTechInternalApi);
    }

    @Override
    protected String getSourceType() {
        return "scraper";
    }

    @Override
    protected String getJobName() {
        return "emerging-tech.scraper.job";
    }
}
