package com.qtai.domain.sharing.internal;

/**
 * 공유 스냅샷 엔티티.
 *
 * 원본 시점의 데이터를 JSON으로 복제 저장 — 원본 변경/삭제와 무관하게 보존.
 * 신고 처리 시 이 스냅샷이 증거가 되므로 모든 공유는 반드시 스냅샷을 가진다.
 */
// TODO: @Entity, @Table(name = "share_snapshot")
public class ShareSnapshot {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: String resourceType;
    // TODO: Long originalResourceId;       — 원본 ID 참조 (추적용, 원본 변경되어도 스냅샷은 불변)
    // TODO: @Column(columnDefinition="JSON") String snapshotJson;
    //       resourceType별 직렬화 스키마는 별도 정의
    // TODO: LocalDateTime createdAt;       — @CreationTimestamp
}
