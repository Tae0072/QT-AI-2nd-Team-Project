package com.qtai.domain.study.internal;

/**
 * 스터디 도메인 진입점. 2개 UseCase(읽기 전용) 구현 + 트랜잭션 경계.
 *
 * 운영 정책 (v3.1):
 *   - 운영자 큐레이션만 — AI 자동 생성 금지
 *   - 등록/수정 API는 별도(관리자 도구) — 본 도메인은 조회만 노출
 *
 * 타 도메인 접근은 client/ 어댑터로만:
 *   - bible.GetBibleVerseUseCase  — 인용 절 본문 동봉용
 *   - member.GetMemberUseCase     — 작성자(ADMIN) 정보 표시
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements GetStudyUseCase, ListStudyUseCase
public class StudyService {

    // TODO: final StudyRepository studyRepository;
    // TODO: final GetBibleVerseUseCase getBibleVerseUseCase;
    // TODO: final GetMemberUseCase getMemberUseCase;

    // TODO: getStudy(studyId) — 없으면 INVALID_INPUT
    // TODO: list(keyword, category, pageable) 구현
}
