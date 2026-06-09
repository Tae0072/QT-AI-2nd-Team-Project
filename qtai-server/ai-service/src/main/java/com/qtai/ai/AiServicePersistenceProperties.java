package com.qtai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "qtai.ai.persistence")
public record AiServicePersistenceProperties(
        boolean enabled,
        String url,
        String username,
        String password,
        String driverClassName,
        String ddlAuto,
        String dialect
) {

    String requireUrl() {
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException(
                    "qtai.ai.persistence.url must be configured when qtai.ai.persistence.enabled=true"
            );
        }
        return url;
    }

    String ddlAutoOrDefault() {
        if (StringUtils.hasText(ddlAuto)) {
            return ddlAuto;
        }
        return "validate";
    }

    boolean hasUsername() {
        return StringUtils.hasText(username);
    }

    boolean hasPassword() {
        return StringUtils.hasText(password);
    }

    boolean hasDriverClassName() {
        return StringUtils.hasText(driverClassName);
    }

    boolean hasDialect() {
        return StringUtils.hasText(dialect);
    }
}
