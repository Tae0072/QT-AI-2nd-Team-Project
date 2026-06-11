package com.qtai.domain.study.api;

import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipCommand;
import com.qtai.domain.study.api.dto.PublishApprovedSimulatorClipResult;

/**
 * AI 산출물 승인본으로부터 사용자 노출용 시뮬레이터 클립을 게시한다 (P1-11).
 *
 * <p>해설(VerseExplanation)에는 publish/hide 경로가 있었으나 SIMULATOR에는 APPROVED row를
 * 만드는 경로가 전혀 없어, 승인해도 사용자 시뮬레이터 버튼(READY)이 켜지지 않는 비대칭이 있었다.
 * 이 UseCase가 ai 승인 흐름에서 호출되어 simulator_clips에 APPROVED 클립을 만든다.
 *
 * <p><b>인가 계약(중요):</b> 이 UseCase는 도메인 내부 계약이며 HTTP 인가를 직접 수행하지 않는다.
 * 게시는 사용자 노출 콘텐츠를 생성하는 관리자 행위이므로, 호출자(ai/admin web 컨트롤러)는
 * 호출 전 반드시 {@code members.role=ADMIN}과 {@code admin_users.admin_role}(REVIEWER 또는
 * SUPER_ADMIN)을 검증해야 한다. 시스템 배치({@code SYSTEM_BATCH}) 외 일반 USER 토큰으로의 직접
 * 호출은 허용하지 않으며, 이 계약은 호출자 측 게이팅 테스트로 보장한다. 서비스 자체는 입력·상태
 * 가드(아래 검증)만 책임진다.
 */
public interface PublishApprovedSimulatorClipUseCase {

    PublishApprovedSimulatorClipResult publishApprovedSimulatorClip(
            PublishApprovedSimulatorClipCommand command
    );
}
