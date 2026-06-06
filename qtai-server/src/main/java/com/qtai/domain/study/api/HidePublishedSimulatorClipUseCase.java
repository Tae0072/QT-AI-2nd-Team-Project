package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.HidePublishedSimulatorClipResult;

/**
 * 노출 중인 시뮬레이터 클립을 숨긴다 (P1-11 긴급 차단).
 *
 * <p>ai 산출물 HIDE 시 simulator_clips가 APPROVED로 남아 사용자 노출이 계속되던 비대칭을 해소.
 *
 * <p><b>인가 계약(중요):</b> 이 UseCase는 도메인 내부 계약이며 HTTP 인가를 직접 수행하지 않는다.
 * 숨김(긴급 차단)은 관리자 행위이므로, 호출자(ai/admin web 컨트롤러)는 호출 전 반드시
 * {@code admin_users.admin_role}(OPERATOR 또는 SUPER_ADMIN)을 검증해야 한다. 일반 USER 토큰의
 * 직접 호출은 허용하지 않으며, 이 계약은 호출자 측 게이팅 테스트로 보장한다.
 */
public interface HidePublishedSimulatorClipUseCase {

    HidePublishedSimulatorClipResult hidePublishedSimulatorClip(
            HidePublishedSimulatorClipCommand command
    );
}
