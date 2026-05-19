package com.qtai.domain.sharing.api;

/**
 * 공유 생성 UseCase 포트.
 *
 * 핵심 정책: 공유 시점에 원본을 ShareSnapshot으로 복제(immutable).
 * 이후 원본이 수정·삭제되어도 공유받은 사람이 보는 내용은 그대로 유지된다.
 * → 원본 수정으로 의도와 다른 내용이 노출되는 문제 차단.
 */
public interface CreateShareUseCase {

    // TODO: SharedSnapshotResponse createShare(Long memberId, ShareCreateRequest request);
    //       리소스 타입(QT/NOTE/STUDY)별 client/ 어댑터로 원본 조회 → 스냅샷 INSERT
}
