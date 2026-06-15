package com.qtai.domain.appversion.api;

import com.qtai.domain.appversion.api.dto.AppVersionStateResponse;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateCreateRequest;
import com.qtai.domain.appversion.api.dto.PendingAppUpdateResponse;
import java.util.List;

/**
 * 관리자 앱 버전/업데이트 관리 UseCase 포트 (2026-06-14 Lead 승인).
 *
 * <p>두 갈래로 나눈다.
 * <ul>
 *   <li>콘텐츠 버전 즉시 게시({@link #applyContent()}) — 백그라운드 데이터 갱신으로 바로 반영.</li>
 *   <li>앱 출시 버전 업데이트 — '업데이트 예정' 항목을 모았다가({@link #createPending}) 적용
 *       ({@link #applyPending})하면 앱 재설치가 필요한 변경이 반영된다.</li>
 * </ul>
 */
public interface AdminAppVersionUseCase {

    /** 현재 버전 상태 조회(없으면 기본값 생성). */
    AppVersionStateResponse getState();

    /** 콘텐츠 버전을 한 단계 올려 즉시 게시한다(예: 0.1.0 → 0.1.0.1). */
    AppVersionStateResponse applyContent();

    /** 업데이트 예정 목록(status = PENDING | APPLIED | null=전체). */
    List<PendingAppUpdateResponse> listPending(String status);

    /** 업데이트 예정 항목 등록. */
    PendingAppUpdateResponse createPending(PendingAppUpdateCreateRequest request);

    /** 업데이트 예정 항목 적용 — 앱 출시 버전을 올리고 항목을 APPLIED로 바꾼다. */
    AppVersionStateResponse applyPending(Long id);

    /** 업데이트 예정 항목 삭제(소프트). */
    void deletePending(Long id);
}
