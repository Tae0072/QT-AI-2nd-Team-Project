package com.qtai.domain.note.internal;

/**
 * 적재된 journal_event를 후속 소비자에게 전달하는 훅(P1-10 재처리기의 처리 단계).
 *
 * <p>{@link JournalEventReprocessor}가 due 이벤트마다 등록된 모든 핸들러를 호출한 뒤 PROCESSED로 전이한다.
 * 핸들러가 예외를 던지면 해당 이벤트는 백오프 후 재시도된다(재시도 상한 도달 시 dead-letter로 보존).
 *
 * <p>현재 운영에는 등록 핸들러가 없다(no-op). 묵상 달력·미션 집계는 note를 직접 읽는 라이브 경로가 담당하고,
 * 본 아웃박스는 "원자적 기록 + 전달 보장"이 1차 목적이기 때문이다. 추후 증분 집계 소비자는
 * {@code previousQtPassageId}/{@code currentStatus}를 활용해 이 인터페이스로 추가한다(같은 도메인 내부).
 */
interface JournalEventDeliveryHandler {

    void deliver(JournalEvent event);
}
