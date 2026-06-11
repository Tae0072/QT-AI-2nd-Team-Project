package com.qtai.domain.qt.api;

import com.qtai.domain.qt.api.dto.BiblePassageStudy;

/**
 * 성경 목차에서 선택한 본문 범위의 해설 진입점 가용성을 조회하는 유스케이스.
 *
 * <p>bible 도메인은 qt·study에 의존할 수 없어(모듈 순환 방지) 해설 가용성을 제공하지 못한다.
 * 대신 qt 도메인이 자신의 본문 매핑(qt_passage_verses)과 study 가용성을 조합해 노출한다.
 * 사용자 앱의 성경 본문 전체 페이지가 이 결과로 해설 버튼 활성 여부를 정한다(F-01·F-08).
 */
public interface GetBiblePassageStudyUseCase {

    /**
     * @param bookCode  성경 권 코드(예: GEN)
     * @param chapter   장
     * @param verseFrom 시작 절
     * @param verseTo   끝 절
     * @return 해설 진입점 가용성 — 매핑/해설이 없으면 {@link BiblePassageStudy#NONE}
     */
    BiblePassageStudy getPassageStudy(String bookCode, int chapter, int verseFrom, int verseTo);
}
