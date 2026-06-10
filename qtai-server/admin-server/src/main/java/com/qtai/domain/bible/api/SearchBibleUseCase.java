package com.qtai.domain.bible.api;

/**
 * 성경 키워드 검색 UseCase 포트.
 *
 * 정책: MySQL FULLTEXT 인덱스만 사용 (RAG/Vector DB 금지 — 인프라 단순화).
 * 결과 페이징 필수 — 매칭 결과가 수천 건 단위일 수 있음.
 */
public interface SearchBibleUseCase {

    // TODO: Page<BibleVerseResponse> search(BibleSearchRequest request, Pageable pageable);
}
