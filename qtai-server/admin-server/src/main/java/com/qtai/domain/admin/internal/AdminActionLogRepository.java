package com.qtai.domain.admin.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 관리자 액션 감사 로그 Repository.
 *
 * <p>INSERT only. 조회는 감사 목적으로만 사용한다.
 */
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {
}
