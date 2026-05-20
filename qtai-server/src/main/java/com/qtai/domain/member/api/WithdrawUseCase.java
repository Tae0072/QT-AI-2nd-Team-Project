package com.qtai.domain.member.api;

/**
 * 회원 탈퇴 UseCase 포트.
 *
 * 정책: 즉시 hard delete 대신 status=WITHDRAWN으로 전환 + 개인정보 익명화
 * (닉네임/이메일/카카오ID → "탈퇴회원_{seq}" 형태). 작성한 QT/노트는 정책에 따라
 * 익명 노출 유지 또는 일괄 PRIVATE 전환.
 */
public interface WithdrawUseCase {

    // TODO: void withdraw(Long memberId, String reason);
    //       1) status=WITHDRAWN, 익명화
    //       2) 본인 콘텐츠 처리 (정책)
    //       3) AuditLog 기록 (탈퇴 사유)
}
