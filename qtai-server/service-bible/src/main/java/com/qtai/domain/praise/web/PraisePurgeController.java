package com.qtai.domain.praise.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.praise.api.PurgeMemberPraiseDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 찬양 저장 데이터 정리 — 서비스 간 <b>내부 배치(SYSTEM_BATCH) 전용</b> 엔드포인트.
 *
 * <p>service-user의 보존기간 만료 회원 정리 배치({@code MemberRetentionPurgeService})가 호출한다.
 * 회원 찬양 저장(member_praise_songs) hard delete라 사용자 요청 경로로 노출하면 안 되므로
 * {@code @PreAuthorize("hasRole('SYSTEM_BATCH')")}로 시스템 배치 호출에만 허용한다(일반 사용자·ADMIN은 403).
 * 시스템 토큰 검증은 {@code JwtAuthenticationFilter} HS256 폴백(PR #440).
 *
 * <p>분산 트랜잭션 없음(회의록 §3): 삭제는 service-bible 자체 트랜잭션({@code PraisePurgeService} {@code @Transactional})에서
 * 커밋된다. 멱등이라 배치 재실행 시 잔여분을 마저 삭제한다.
 */
@RestController
@RequiredArgsConstructor
public class PraisePurgeController {

    private final PurgeMemberPraiseDataUseCase purgeMemberPraiseDataUseCase;

    /** 해당 회원의 찬양 저장 데이터를 삭제하고 삭제 행 수를 돌려준다. */
    @PostMapping("/api/v1/praise-songs/purge")
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<Integer> purge(@RequestParam Long memberId) {
        return ApiResponse.success(purgeMemberPraiseDataUseCase.purgeByMemberId(memberId));
    }
}
