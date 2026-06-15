package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.AdminNoteDetail;
import com.qtai.domain.note.api.dto.AdminNoteItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 관리자 회원 상세 — 회원이 작성한 노트 목록/상세 조회 UseCase 포트.
 *
 * <p>도메인 경계: member 도메인이 note 내부(Note/Repository)를 직접 호출하지 않고 이 포트로만 본다.
 */
public interface ListMemberNotesForAdminUseCase {

    /** 회원이 작성한(삭제 제외) 노트를 최신순으로 페이징 조회한다. */
    Page<AdminNoteItem> listNotesByMember(Long memberId, Pageable pageable);

    /** 노트 1건의 전체 내용을 조회한다. 해당 회원의 노트가 아니면 NOTE_NOT_FOUND. */
    AdminNoteDetail getNoteDetailForMember(Long memberId, Long noteId);
}
