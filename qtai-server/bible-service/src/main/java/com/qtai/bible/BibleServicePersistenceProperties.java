package com.qtai.bible;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * bible-service 전용 DB(DB-per-service) 설정. {@code qtai.bible.persistence.*}.
 * skeleton 단계는 enabled=false라 모놀리식 DB를 그대로 쓰며, 본 서비스는 구조만 보유한다.
 */
@ConfigurationProperties(prefix = "qtai.bible.persistence")
public record BibleServicePersistenceProperties(
        boolean enabled,
        String url,
        String username,
        String password,
        String driverClassName,
        String ddlAuto,
        String dialect,
        boolean flywayEnabled,
        String flywayLocations
) {

    String requireUrl() {
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException(
                    "qtai.bible.persistence.url must be configured when qtai.bible.persistence.enabled=true"
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

    String flywayLocationsOrDefault() {
        if (StringUtils.hasText(flywayLocations)) {
            return flywayLocations;
        }
        return "classpath:db/migration";
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
