package com.qtai.domain.qt.internal;

import com.qtai.domain.bible.api.ListBibleBooksUseCase;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * qt 도메인에서 bible 권(book) 메타를 조회하는 어댑터(리뷰 §5.2 #1, MSA 경계).
 *
 * <p>기존 {@code QtPassageRepository}의 native SQL이 bible 소유 테이블({@code bible_books})을 직접
 * 조회/JOIN하던 경계 침범을 제거하고, bible의 공개 포트 {@link ListBibleBooksUseCase}를 통해서만 접근한다.
 * note/ai 도메인이 bible api를 직접 주입하는 기존 패턴과 동일하다.
 *
 * <p>성경 권은 66개 고정 참조 데이터이고, 호출 지점(Today QT 캐시 갱신·일일 수집)이 사용자 요청당이 아니라
 * 배치/캐시 경계라 전권 조회 비용은 무시할 수준이다(캐싱은 별도 개선 항목).
 */
@Component
@RequiredArgsConstructor
class BibleBookLookup {

    private final ListBibleBooksUseCase listBibleBooksUseCase;

    /** 영문 권명으로 book id 조회(정확 일치). 미존재 시 empty. */
    Optional<Short> findBookIdByEnglishName(String englishName) {
        if (englishName == null) {
            return Optional.empty();
        }
        return listBibleBooksUseCase.listBibleBooks().stream()
                .filter(book -> englishName.equals(book.englishName()))
                .map(BibleBookLookup::toShortId)
                .filter(id -> id != null)
                .findFirst();
    }

    /** book id로 권 메타 조회. 미존재 시 empty. */
    Optional<BibleBookResponse> findById(Short bookId) {
        if (bookId == null) {
            return Optional.empty();
        }
        return listBibleBooksUseCase.listBibleBooks().stream()
                .filter(book -> book.id() != null && bookId.intValue() == book.id().intValue())
                .findFirst();
    }

    private static Short toShortId(BibleBookResponse book) {
        return book.id() == null ? null : book.id().shortValue();
    }
}
