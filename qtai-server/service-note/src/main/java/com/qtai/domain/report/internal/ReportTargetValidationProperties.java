package com.qtai.domain.report.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "qtai.report.target-validation")
public class ReportTargetValidationProperties {

    private boolean aiQaRequestEnabled;

    public boolean isAiQaRequestEnabled() {
        return aiQaRequestEnabled;
    }

    public void setAiQaRequestEnabled(boolean aiQaRequestEnabled) {
        this.aiQaRequestEnabled = aiQaRequestEnabled;
    }
}
