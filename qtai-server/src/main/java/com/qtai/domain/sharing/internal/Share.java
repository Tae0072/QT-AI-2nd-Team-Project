package com.qtai.domain.sharing.internal;

/**
 * 공유 엔티티 (메타정보).
 *
 * 1:1 관계로 ShareSnapshot과 짝지어진다. Share가 공유의 생애주기·소유자를 관리하고
 * ShareSnapshot이 실제 노출되는 데이터를 보관한다.
 */
// TODO: @Entity, @Table(name = "share")
public class Share {

    // TODO: @Id @GeneratedValue Long id;
    // TODO: Long ownerId;                        — 공유 만든 사람
    // TODO: String resourceType;                 — QT / NOTE / STUDY
    // TODO: Long resourceId;                     — 원본 리소스 ID (취소·통계용)
    // TODO: String shareToken;                   — UUID, unique, URL에 노출됨
    // TODO: Long snapshotId;                     — ShareSnapshot FK (1:1)
    // TODO: LocalDateTime sharedAt;              — @CreationTimestamp
    // TODO: LocalDateTime expiresAt;             — nullable (null=무기한)
    // TODO: boolean revoked;                     — 기본 false
    // TODO: LocalDateTime revokedAt;             — nullable
}
