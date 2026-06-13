package com.qtai.domain.note.api;

/**
 * 노트 나눔 표시 포트 — sharing 도메인이 호출한다(CLAUDE.md §4 도메인 경계).
 *
 * <p>나눔 공개(publish) 시 원본 노트를 {@code SHARED}로, 공개 중단(나눔 글 삭제) 시
 * {@code PRIVATE}로 되돌려, 노트 목록의 {@code shared} 플래그({@code visibility == SHARED})가
 * 실제 공유 상태를 반영하게 한다(기록 화면 '나눔' 필터 근거).
 */
public interface MarkNoteSharedUseCase {

    /** noteId 노트를 SHARED로 표시한다. 노트가 없으면(삭제 등) 아무것도 하지 않는다(멱등). */
    void markShared(Long memberId, Long noteId);

    /** noteId 노트를 PRIVATE로 되돌린다. 노트가 없으면 아무것도 하지 않는다(멱등). */
    void markUnshared(Long memberId, Long noteId);
}
