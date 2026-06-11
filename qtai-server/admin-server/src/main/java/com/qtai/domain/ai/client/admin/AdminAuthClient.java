package com.qtai.domain.ai.client.admin;

import java.util.Collection;

import com.qtai.domain.ai.client.AiClientException;

public interface AdminAuthClient {

    AdminAuthResult getActiveAdmin(Long memberId) throws AiClientException;

    AdminAuthResult verifyRole(Long memberId, AdminRole requiredRole) throws AiClientException;

    AdminAuthResult verifyAnyRole(Long memberId, Collection<AdminRole> requiredRoles) throws AiClientException;

    record AdminAuthResult(
            Long adminUserId,
            Long memberId,
            AdminRole adminRole
    ) {
    }

    enum AdminRole {
        OPERATOR,
        REVIEWER,
        CONTENT_CREATOR,
        SUPER_ADMIN
    }
}
