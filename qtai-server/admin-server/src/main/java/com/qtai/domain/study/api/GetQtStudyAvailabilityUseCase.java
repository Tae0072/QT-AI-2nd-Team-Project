package com.qtai.domain.study.api;

import java.util.List;

import com.qtai.domain.study.api.dto.QtStudyAvailability;

/**
 * Today QT 응답용 학습 콘텐츠 가용성 조회 포트 — qt 도메인 전용 경량 계약.
 *
 * <p>CLAUDE.md §6 "Today QT 100%"는 본문·해설 진입점·노트 진입점·시뮬레이터 상태의
 * 반환을 뜻한다. 기존에는 qt가 simulatorStatus="MISSING"/hasExplanation=false를
 * 하드코딩해 승인 콘텐츠가 있어도 진입점이 영구히 닫혀 있었다.
 *
 * <p>{@code GetQtSimulatorUseCase}는 scene JSON 전체를 싣는 무거운 응답이라
 * Today QT enrich에는 이 상태 전용 포트를 사용한다. verseIds는 호출자(qt)가
 * 이미 보유한 값을 전달해 study→qt 역호출(순환 의존)을 만들지 않는다.
 */
public interface GetQtStudyAvailabilityUseCase {

    /**
     * @param qtPassageId QT 본문 ID
     * @param verseIds    본문의 bible verse id 목록 (해설 존재 판단용)
     * @return 시뮬레이터 상태(READY/MISSING — FAILED/DISABLED 판정은 후속 과제)와
     *         승인 해설 존재 여부
     */
    QtStudyAvailability getAvailability(Long qtPassageId, List<Long> verseIds);
}
