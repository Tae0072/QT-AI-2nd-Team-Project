package com.qtai.domain.ai.client.admin;

import java.util.Collection;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;

@Component("aiAdminAuthClientMock")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "qtai.ai.client.mock.enabled", havingValue = "true")
@ConditionalOnMissingBean(AdminAuthClient.class)
public class AdminAuthClientMock implements AdminAuthClient {

    @Override
    public AdminAuthResult getActiveAdmin(Long memberId) {
        return new AdminAuthResult(1L, memberId, AdminRole.REVIEWER);
    }

    @Override
    public AdminAuthResult verifyRole(Long memberId, AdminRole requiredRole) {
        return new AdminAuthResult(1L, memberId, requiredRole);
    }

    @Override
    public AdminAuthResult verifyAnyRole(Long memberId, Collection<AdminRole> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty() || requiredRoles.stream().anyMatch(Objects::isNull)) {
            throw validationFailure("requiredRoles must not be null, empty, or contain null");
        }
        AdminRole role = requiredRoles.iterator().next();
        return new AdminAuthResult(1L, memberId, role);
    }

    private static AiClientException validationFailure(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, "admin-auth", message);
    }
}
