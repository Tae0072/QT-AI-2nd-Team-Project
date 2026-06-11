package com.qtai.domain.qt.api.dto;

/**
 * 성경 본문 범위의 해설 진입점 가용성.
 *
 * <p>성경 목차에서 선택한 범위가 QT 본문과 매핑되고 승인된 해설(verse_explanations,
 * APPROVED·ACTIVE)이 있으면, 오늘의 QT와 동일하게 해설 진입점을 노출하기 위한 계약이다.
 * 해설 콘텐츠 조회(`/qt/{qtPassageId}/study-content`)에 qtPassageId가 필요하므로,
 * 해설이 있을 때만 qtPassageId를 함께 채운다.
 *
 * @param qtPassageId    해설 콘텐츠 조회용 QT 본문 ID (해설이 없으면 null)
 * @param hasExplanation 승인된 해설 존재 여부 — 해설 진입점 활성 기준
 */
public record BiblePassageStudy(
        Long qtPassageId,
        boolean hasExplanation
) {
    public static final BiblePassageStudy NONE = new BiblePassageStudy(null, false);
}
