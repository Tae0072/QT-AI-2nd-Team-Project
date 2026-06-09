package com.qtai.domain.note.api;

import com.qtai.domain.note.api.dto.NoteListResponse;
import org.springframework.data.domain.Pageable;

// NoteCategory, NoteStatus는 같은 패키지(api)라 import 불필요

/**
 * 노트 목록 조회 UseCase 포트.
 *
 * 강제 필터 (Service 본문에서 무조건 적용):
 * - 본인 노트만 (memberId)
 * - 삭제된 노트 제외 (deletedAt IS NULL)
 *
 * 선택 필터 (모두 nullable):
 * - category: MEDITATION / SERMON / PRAYER / REPENTANCE / GRATITUDE
 * - status: DRAFT / SAVED
 * - q: 키워드 (제목·본문 부분 일치)
 *
 * 호출자: note/web (NoteController). Flutter N-01 화면이 호출.
 */
public interface ListNotesUseCase {

    NoteListResponse list(
            Long memberId,
            NoteCategory category, // nullable — 5개 카테고리 통합 조회 시 null
            NoteStatus status, // nullable — 모든 상태 조회 시 null
            String q, // nullable — 검색어 없으면 null
            Pageable pageable);
}