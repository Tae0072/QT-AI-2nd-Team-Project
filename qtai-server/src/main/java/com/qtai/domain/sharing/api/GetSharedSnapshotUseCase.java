package com.qtai.domain.sharing.api;

/**
 * 공유 스냅샷 조회 UseCase 포트.
 *
 * 비로그인 접근 허용 — 공유 링크를 받은 사람이 회원이 아닐 수 있다.
 * revoked=true이거나 expiresAt 경과 시 404/410.
 * report 도메인이 신고 대상 검증 시에도 이 UseCase 사용.
 */
public interface GetSharedSnapshotUseCase {

    // TODO: SharedSnapshotResponse getSnapshot(String shareToken);
    //       shareToken은 추측 불가능한 UUID/랜덤 문자열
}
