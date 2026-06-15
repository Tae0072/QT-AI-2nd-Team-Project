package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.AdminSimulatorClipListResponse;
import com.qtai.domain.study.api.dto.ListAdminSimulatorClipsQuery;

/**
 * 관리자 시뮬레이터 클립 목록 조회 UseCase (AD 시뮬레이터 관리, F-06/F-12).
 *
 * <p>상태/본문별 필터 + 서버 페이지네이션. 원문(sceneScriptJson)은 노출하지 않는다(메타만).
 */
public interface ListAdminSimulatorClipsUseCase {

    AdminSimulatorClipListResponse listAdminSimulatorClips(ListAdminSimulatorClipsQuery query);
}
