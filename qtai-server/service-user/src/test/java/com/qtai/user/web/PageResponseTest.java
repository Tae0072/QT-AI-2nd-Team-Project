package com.qtai.user.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * {@link PageResponse} 단위 테스트 — 04 API 명세서 §1.6 sort 직렬화 형식 검증.
 */
class PageResponseTest {

    @Test
    void 미정렬이면_sort는_빈문자열() {
        PageResponse<String> r = PageResponse.from(
                new PageImpl<>(List.of("a"), PageRequest.of(0, 20), 1));

        assertThat(r.sort()).isEmpty();
        assertThat(r.content()).containsExactly("a");
        assertThat(r.page()).isZero();
        assertThat(r.totalElements()).isEqualTo(1);
        assertThat(r.first()).isTrue();
        assertThat(r.last()).isTrue();
    }

    @Test
    void 단일정렬은_property_direction_소문자() {
        PageResponse<String> r = PageResponse.from(new PageImpl<>(
                List.of("a"), PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"))), 1));

        assertThat(r.sort()).isEqualTo("createdAt,desc");
    }

    @Test
    void 다중정렬은_콤마로_이어붙인다() {
        PageResponse<String> r = PageResponse.from(new PageImpl<>(
                List.of("a"),
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))),
                1));

        assertThat(r.sort()).isEqualTo("createdAt,desc,id,asc");
    }
}
