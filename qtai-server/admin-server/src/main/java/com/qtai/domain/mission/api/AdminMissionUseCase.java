package com.qtai.domain.mission.api;

import com.qtai.domain.mission.api.dto.AdminMissionResponse;
import com.qtai.domain.mission.api.dto.MissionCreateRequest;
import com.qtai.domain.mission.api.dto.MissionUpdateRequest;
import java.util.List;

/**
 * 관리자 미션 정의 관리 UseCase 포트 (F-13).
 *
 * <p>미션 정의의 목록·생성·수정·상태변경(ACTIVE/HIDDEN)을 제공한다.
 * 회원 진행률 집계 로직은 기존 mission 도메인 서비스가 담당한다.
 */
public interface AdminMissionUseCase {

    List<AdminMissionResponse> listForAdmin();

    AdminMissionResponse getForAdmin(Long id);

    AdminMissionResponse create(MissionCreateRequest request);

    AdminMissionResponse update(Long id, MissionUpdateRequest request);

    AdminMissionResponse changeStatus(Long id, String status);
}
