package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.ListNotesUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteListItem;
import com.qtai.domain.note.api.dto.NoteListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 노트 도메인 진입점. UseCase 구현 + 트랜잭션 경계.
 *
 * 권한 정책:
 * - 조회: 본인 노트만 (memberId 강제 필터) + deletedAt IS NULL (Repository에서 적용)
 * - 작성/수정/삭제: 작성자 본인만 (FORBIDDEN) — 다음 PR
 *
 * 매핑 정책 (이번 PR):
 * - visibility = "PRIVATE" 고정 (B2 노트 생성 PR에서 Entity 필드 추가 후 진짜 값으로 교체)
 * - qtDate, rangeLabel = null (다음 PR에서 qt 도메인 batch 조회로 보강)
 * - shared = false (W3 나눔 공개 PR에서 진짜 값으로 교체)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService implements ListNotesUseCase {

    private static final String DEFAULT_VISIBILITY = "PRIVATE";
    private static final String DEFAULT_SORT = "updatedAt,desc";

    private final NoteRepository noteRepository;

    @Override
    public NoteListResponse list(Long memberId, NoteCategory category, NoteStatus status, String q, Pageable pageable) {
        // 사용자 입력에 %, _ 같은 LIKE 와일드카드가 포함돼도 의도와 다른 결과가 나오지 않도록 이스케이프.
        // Repository JPQL은 ESCAPE '\\' 절을 명시한다.
        Page<Note> page = noteRepository.search(memberId, category, status, escapeLikeWildcards(q), pageable);

        List<NoteListItem> content = page.getContent().stream()
                .map(note -> toListItem(note))
                .toList();

        return new NoteListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                formatSort(pageable.getSort())
        );
    }

    private NoteListItem toListItem(Note note) {
        return new NoteListItem(
                note.getId(),
                note.getCategory(),
                note.getTitle(),
                note.getStatus(),
                DEFAULT_VISIBILITY,
                null,
                null,
                false,
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private String formatSort(Sort sort) {
        return sort.stream()
                .findFirst()
                .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase())
                .orElse(DEFAULT_SORT);
    }

    /**
     * LIKE 와일드카드(%, _) 이스케이프. 백슬래시를 먼저 처리해야 중복 이스케이프 방지.
     * Repository JPQL은 ESCAPE '\\' 절을 명시해 이스케이프된 문자를 리터럴로 해석한다.
     *
     * <p>q가 null이거나 공백만 있으면 null을 반환해 JPQL의 (:q IS NULL) 분기가
     * 검색 조건 자체를 우회하도록 한다. (빈 문자열 → LIKE '%%' 전체 매치 사고 방지)
     */
    private static String escapeLikeWildcards(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
