package com.qtai.domain.audit.api;

import com.qtai.domain.audit.api.dto.AuditLogFilter;
import com.qtai.domain.audit.api.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 감사 로그 조회 UseCase 포트.
 *
 * <p>호출자는 admin 도메인(관리자) 한정. 일반 사용자에게 노출 금지.
 * 필터(행위자/액션/기간)와 페이징을 적용해 최신순으로 반환한다.
 */
public interface ListAuditUseCase {

    Page<AuditLogResponse> list(AuditLogFilter filter, Pageable pageable);
}
