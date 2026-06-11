package com.qtai.domain.admin.internal;

/**
 * 관리자 계정 상태 enum.
 *
 * <p>ERD: admin_users.status 컬럼에 매핑.
 */
public enum AdminStatus {

    /** 활성 관리자. */
    ACTIVE,

    /** 비활성 (권한 정지). */
    DISABLED
}
