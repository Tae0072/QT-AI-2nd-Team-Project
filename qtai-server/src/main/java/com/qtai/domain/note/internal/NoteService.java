package com.qtai.domain.note.internal;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.dto.NoteListResponse;
import lombok.RequiredArgsConstructor;

/**
 * 노트 도메인 진입점. 4개 UseCase 구현 + 트랜잭션 경계.
 *
 * 권한 정책:
 * - 조회: 종속 QT의 가시 권한과 동일 (qt가 PRIVATE이면 작성자만)
 * - 작성/수정/삭제: 작성자 본인만 (FORBIDDEN)
 *
 * 타 도메인 접근:
 * - member.GetMemberUseCase → client/member 어댑터
 * - qt.GetQtUseCase → client/qt 어댑터 (qt 존재/권한 검증용)
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements CreateNoteUseCase, GetNoteUseCase, UpdateNoteUseCase,
// DeleteNoteUseCase
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService implements ListNotesUseCase {
    @Override
    public NoteListResponse list(Long memberId, NoteCategory category, NoteStatus status, String q, Pageable pageable) {
        // TODO: 이승욱 Entity·Repository 머지 후 구현
        // 1. noteRepository.findBy... (memberId, deletedAt IS NULL, category, status,
        // q, pageable)
        // 2. Page<Note> → List<NoteListItem> 매핑
        // 3. new NoteListResponse(content, page, size, totalElements, totalPages,
        // first, last)
        throw new UnsupportedOperationException("TODO: 이승욱 Entity 머지 후 구현");
    }

    // TODO: final NoteRepository noteRepository;
    // TODO: final GetMemberUseCase getMemberUseCase;
    // TODO: final GetQtUseCase getQtUseCase;

    // TODO: @Transactional createNote — qt 존재/소유 검증 후 INSERT
    // TODO: getNote / listByQt — qt 가시 권한 검증
    // TODO: @Transactional updateNote — 작성자 검증 후 수정
    // TODO: @Transactional deleteNote — 작성자 검증 후 삭제
}
