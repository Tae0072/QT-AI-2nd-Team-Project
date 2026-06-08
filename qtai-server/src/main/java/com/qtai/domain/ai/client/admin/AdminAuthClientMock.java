package com.qtai.domain.ai.client.admin;

import java.util.Collection;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
        AdminRole role = requiredRoles == null || requiredRoles.isEmpty()
                ? AdminRole.SUPER_ADMIN
                : requiredRoles.iterator().next();
        return new AdminAuthResult(1L, memberId, role);
    }
}
