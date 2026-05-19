package com.qtai.domain.member.internal;

/**
 * 회원 도메인 진입점. 5개 UseCase 구현 + 트랜잭션 경계.
 *
 * 외부 의존:
 *   - external.kakao.KakaoOAuthClient (포트) — 직접 주입 (OAuth는 member의 핵심 책임)
 *   - JWT 발급/검증 유틸 (별도 컴포넌트로 분리 권장)
 *
 * 정책:
 *   - 첫 로그인 자동 가입 (kakaoId로 조회 후 없으면 INSERT)
 *   - 닉네임 unique 제약 → 첫 가입 시 카카오 닉네임 + suffix로 자동 생성 후 사용자가 수정
 *   - 탈퇴는 hard delete 아닌 status=WITHDRAWN + 익명화
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements GetMemberUseCase, LoginUseCase, LogoutUseCase, UpdateProfileUseCase, WithdrawUseCase
public class MemberService {

    // TODO: final MemberRepository memberRepository;
    // TODO: final KakaoOAuthClient kakaoOAuthClient;
    // TODO: final JwtTokenProvider jwtTokenProvider; (직접 구현 또는 별도 컴포넌트)

    // TODO: getMember(id) — Optional 검사 후 MemberResponse 변환
    // TODO: @Transactional login(request) — Kakao 검증 → 회원 조회/생성 → JWT 발급
    // TODO: @Transactional logout(memberId, token) — blacklist 등록
    // TODO: @Transactional updateProfile(memberId, request) — 닉네임 중복 체크 후 수정
    // TODO: @Transactional withdraw(memberId, reason) — 익명화 + 상태 전환 + AuditLog
}
