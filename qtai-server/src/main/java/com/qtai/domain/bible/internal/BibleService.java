package com.qtai.domain.bible.internal;

/**
 * 성경 도메인 진입점. 3개 UseCase 구현 + 트랜잭션 경계.
 *
 * 데이터 소스: data/bible-sources/ 시드 데이터를 MySQL FULLTEXT 인덱스로 색인.
 * RAG/Vector DB 도입 금지 — 인프라 단순성 우선.
 *
 * 캐싱: 책/장 목록은 변경 거의 없으므로 @Cacheable 적용 권장.
 */
// TODO: @Service, @RequiredArgsConstructor, @Transactional(readOnly = true)
// TODO: implements GetBibleVerseUseCase, ListChaptersUseCase, SearchBibleUseCase
public class BibleService {

    // TODO: final BibleRepository bibleRepository;

    // TODO: getVerse / getVerses 구현 — 없으면 throw BusinessException(INVALID_INPUT)
    // TODO: listBooks / listChapters 구현 — @Cacheable("bible-books")
    // TODO: search(request, pageable) 구현 — FULLTEXT 쿼리 호출 후 DTO 매핑
}
