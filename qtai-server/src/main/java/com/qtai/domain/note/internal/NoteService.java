package com.qtai.domain.note.internal;

/**
 * 노트 도메인 진입점. 4개 UseCase 구현 + 트랜잭션 경계.
 *
 * 권한 정책:
 *   - 조회: 종속 QT의 가시 권한과 동일 (qt가 PRIVATE이면 작성자만)
 *   - 작성/수정/삭제: 작성자 본인만 (FORBIDDEN)
 *
 * 타 도메인 접근:
 *   - member.GetMemberUseCase → client/member 어댑터
 *   - qt.GetQtUseCase        → client/qt 어댑터 (qt 존재/권한 검증용)
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements CreateNoteUseCase, GetNoteUseCase, UpdateNoteUseCase, DeleteNoteUseCase
public class NoteService {

    // TODO: final NoteRepository noteRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;
    // TODO: final GetQtUseCase getQtUseCase;

    // TODO: @Transactional createNote — qt 존재/소유 검증 후 INSERT
    // TODO: getNote / listByQt — qt 가시 권한 검증
    // TODO: @Transactional updateNote — 작성자 검증 후 수정
    // TODO: @Transactional deleteNote — 작성자 검증 후 삭제
}
