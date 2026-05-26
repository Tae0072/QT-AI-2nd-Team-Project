package com.qtai.domain.note.api;

/**
 * 노트 작성 UseCase 포트.
 *
 * 노트는 특정 QT에 종속된 추가 메모(예: "오늘 다시 보니 의미가 달랐다").
 * QT 1건당 노트 N건. QT 작성자만 노트 작성 가능 (정책 변경 가능).
 */
public interface CreateNoteUseCase {

    // TODO: NoteResponse createNote(Long memberId, Long qtId, NoteCreateRequest request);
    //       qt 존재/소유자 검증 후 INSERT
}
