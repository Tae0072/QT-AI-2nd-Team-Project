package com.qtai.domain.study.internal;

/**
 * 스터디 영속성 포트. Spring Data JPA로 구현.
 */
public interface StudyRepository {

    // TODO: extends JpaRepository<Study, Long>
    // TODO: Page<Study> findByCategoryAndTitleContaining(String category, String title, Pageable pageable);
    //       카테고리 null이면 전체, 키워드 null이면 카테고리만 필터 — 동적 쿼리 필요 시 Specification 활용
}
