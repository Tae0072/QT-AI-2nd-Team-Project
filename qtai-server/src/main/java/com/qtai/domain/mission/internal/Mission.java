package com.qtai.domain.mission.internal;

/**
 * 미션 엔티티.
 *
 * 한 회원이 같은 type의 미션을 동시에 두 개 IN_PROGRESS 상태로 둘 수 없다
 * (DB unique 또는 서비스 레이어 검증).
 */
// TODO: @Entity, @Table(name = "mission")
public class Mission {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long memberId;
    // TODO: String type;                       — DAILY_QT / WEEKLY_BIBLE ...
    // TODO: @Enumerated(STRING) MissionStatus status;  — IN_PROGRESS / COMPLETED / EXPIRED
    // TODO: Integer progress;                  — 현재 진행 횟수
    // TODO: Integer target;                    — 목표 횟수
    // TODO: LocalDateTime startedAt;           — @CreationTimestamp
    // TODO: LocalDateTime completedAt;         — 완료 시 세팅
}
