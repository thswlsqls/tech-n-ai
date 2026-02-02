package com.tech.n.ai.batch.source.domain.emergingtech.github.writer;

import com.tech.n.ai.batch.source.domain.emergingtech.writer.AbstractEmergingTechWriter;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import org.springframework.batch.core.configuration.annotation.StepScope;

/**
 * GitHub Releases → Emerging Tech Internal API Writer
 */
@StepScope
public class GitHubReleasesWriter extends AbstractEmergingTechWriter {

    public GitHubReleasesWriter(EmergingTechInternalContract emergingTechInternalApi) {
        super(emergingTechInternalApi);
    }

    @Override
    protected String getSourceType() {
        return "GitHub";
    }

    @Override
    protected String getJobName() {
        return "emerging-tech.github.job";
    }
}
