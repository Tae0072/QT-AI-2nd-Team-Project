package com.qtai.domain.note.api;

/**
 * 회원 보존기간 만료 정리 — note 도메인 데이터 삭제 포트.
 *
 * <p>탈퇴 후 2년(보존기간) 경과 회원의 노트 관련 데이터
 * (journal_events, note_verses, notes)를 hard delete한다.
 * member 도메인의 보존기간 만료 배치(SYSTEM_BATCH)에서만 호출하며,
 * 사용자 요청 경로에서 호출하지 않는다.
 *
 * <p>호출자는 sharing 도메인 정리(나눔 글의 note FK 해제)를 먼저 수행해야 한다.
 */
public interface PurgeMemberNoteDataUseCase {

    /**
     * 해당 회원의 노트 데이터를 모두 삭제한다 (호출자 트랜잭션에 참여).
     *
     * @param memberId 대상 회원 ID
     * @return 삭제된 행 수 합계
     */
    int purgeByMemberId(Long memberId);
}
