package com.qtai.domain.ai.client.admin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.http.AiClientProperties;
import com.qtai.domain.ai.client.http.AiHttpSupport;

@Component("aiAdminAuthClientHttpAdapter")
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "http")
@ConditionalOnMissingBean(AdminAuthClient.class)
public class AdminAuthClientHttpAdapter implements AdminAuthClient {

    private static final String DOWNSTREAM = "admin-auth";
    private static final String ACTIVE_PATH = "/api/v1/system/admin/auth/active";
    private static final String VERIFY_PATH = "/api/v1/system/admin/auth/verify";
    private static final String VERIFY_ANY_PATH = "/api/v1/system/admin/auth/verify-any";

    private final AiHttpSupport http;
    private final JavaType resultType;

    public AdminAuthClientHttpAdapter(ObjectMapper objectMapper, AiClientProperties properties) {
        this.http = new AiHttpSupport(objectMapper, properties, properties.getAdminAuth(), DOWNSTREAM);
        this.resultType = objectMapper.getTypeFactory().constructType(AdminAuthResult.class);
    }

    @Override
    public AdminAuthResult getActiveAdmin(Long memberId) {
        Map<String, Object> queryParameters = memberIdQuery(memberId);
        return http.get(ACTIVE_PATH, queryParameters, resultType);
    }

    @Override
    public AdminAuthResult verifyRole(Long memberId, AdminRole requiredRole) {
        if (requiredRole == null) {
            throw validationFailure("requiredRole must not be null");
        }
        Map<String, Object> queryParameters = memberIdQuery(memberId);
        queryParameters.put("role", requiredRole.name());
        return http.get(VERIFY_PATH, queryParameters, resultType);
    }

    @Override
    public AdminAuthResult verifyAnyRole(Long memberId, Collection<AdminRole> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty() || requiredRoles.stream().anyMatch(Objects::isNull)) {
            throw validationFailure("requiredRoles must not be null, empty, or contain null");
        }
        Map<String, Object> queryParameters = memberIdQuery(memberId);
        queryParameters.put("roles", requiredRoles.stream()
                .map(AdminRole::name)
                .collect(Collectors.joining(",")));
        return http.get(VERIFY_ANY_PATH, queryParameters, resultType);
    }

    private static Map<String, Object> memberIdQuery(Long memberId) {
        if (memberId == null) {
            throw validationFailure("memberId must not be null");
        }
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        queryParameters.put("memberId", memberId);
        return queryParameters;
    }

    private static AiClientException validationFailure(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, DOWNSTREAM, message);
    }
}
