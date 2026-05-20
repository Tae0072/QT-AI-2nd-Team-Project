package com.qtai.domain.sharing.internal;

/**
 * 공유 도메인 진입점. 3개 UseCase 구현 + 트랜잭션 경계.
 *
 * 타 도메인 원본은 client/ 어댑터로만:
 *   - qt.GetQtUseCase / note.GetNoteUseCase / study.GetStudyUseCase
 *   - member.GetMemberUseCase (공유자 검증)
 *
 * 스냅샷 정책: 공유 생성 시 원본을 JSON 직렬화해 ShareSnapshot에 보존.
 * 원본 수정/삭제 후에도 공유받은 사람은 스냅샷 시점의 데이터를 본다.
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements CreateShareUseCase, GetSharedSnapshotUseCase, RevokeShareUseCase
public class SharingService {

    // TODO: final ShareRepository shareRepository;
    // TODO: final ShareSnapshotRepository shareSnapshotRepository; (또는 통합)
    // TODO: final GetMemberUseCase / GetQtUseCase / GetNoteUseCase / GetStudyUseCase

    // TODO: @Transactional createShare 구현
    //       1) resourceType 분기 → 해당 client/ 어댑터로 원본 조회 (가시 권한 검증 포함)
    //       2) 원본 → JSON 직렬화 → ShareSnapshot INSERT
    //       3) shareToken(UUID) 생성 → Share INSERT
    //       4) SharedSnapshotResponse 반환

    // TODO: getSnapshot(shareToken) — 만료/취소 체크 후 스냅샷 반환
    // TODO: @Transactional revokeShare — owner 검증 후 revoked=true 토글
}
