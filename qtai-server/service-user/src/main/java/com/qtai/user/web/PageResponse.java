package com.qtai.user.web;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

/**
 * 표준 페이징 응답 envelope.
 *
 * <p>Spring {@code Page<T>}를 그대로 직렬화하면 내부 구조(pageable, sort 등)가 노출되고
 * Spring 버전에 따라 JSON 형태가 흔들린다. 안정적인 계약을 위해 필요한 필드만 담은
 * 명시적 record로 감싼다(한 방 자동머지 품질 기준: 표준 페이징 envelope).
 *
 * <p>참고: dev-msa 통합 시 PR#2(qt·study)가 도입하는 공통 페이징 envelope와 위치를
 * 합칠 수 있다(현재는 병행 작업 충돌 회피를 위해 service-user 로컬에 둔다).
 *
 * @param content       현재 페이지 데이터
 * @param page          현재 페이지 번호(0-base)
 * @param size          페이지 크기
 * @param totalElements 전체 요소 수
 * @param totalPages    전체 페이지 수
 * @param first         첫 페이지 여부
 * @param last          마지막 페이지 여부
 * @param sort          정렬 기준 문자열. 04 API 명세서 §1.6 형식("property,direction", 다중은
 *                      "p1,dir1,p2,dir2"로 이어붙임, 방향은 소문자). 미정렬이면 빈 문자열.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {

    /** Spring {@link Page}를 표준 envelope로 변환한다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        // 04 명세 §1.6: sort는 "createdAt,desc"처럼 콤마로 이어붙인 단일 문자열(방향 소문자).
        String sort = page.getSort().stream()
                .map(order -> order.getProperty() + ","
                        + order.getDirection().name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                sort);
    }
}
