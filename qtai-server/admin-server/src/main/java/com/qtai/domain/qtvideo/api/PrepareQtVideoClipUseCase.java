package com.qtai.domain.qtvideo.api;

import com.qtai.domain.qtvideo.api.dto.PrepareQtVideoClipResult;

/**
 * QT영상 클립 준비(방식 A — 절별 구간 기반)를 다른 도메인에서 호출하기 위한 UseCase 계약.
 *
 * <p>도메인 경계(CLAUDE.md §3·§4): {@code domain.qt}(관리자 QT 본문 등록/게시)가 본문 공개 시점에
 * 클립을 자동 준비하려면 {@code domain.qtvideo}의 내부 서비스를 직접 import할 수 없으므로 이 api 계약을 통한다.
 * 구현은 {@code AdminQtVideoService.prepareClip}이며, 본문의 절↔타임코드 매핑에서 {@code [min,max]} 구간을
 * 잘라 활성 클립을 생성·교체한다(공개된 본문에서만, 멱등).
 */
public interface PrepareQtVideoClipUseCase {

    /**
     * 해당 QT 본문의 활성 클립을 절별 구간 기반으로 준비한다.
     *
     * @param adminUserId 감사 로그 주체(자동 준비를 유발한 관리자)
     * @param qtPassageId 대상 QT 본문 id
     * @return 준비 결과(준비 여부·클립 id). 미공개/절매핑 없음/구간 없음이면 {@code prepared=false}
     */
    PrepareQtVideoClipResult prepareClip(Long adminUserId, Long qtPassageId);
}
