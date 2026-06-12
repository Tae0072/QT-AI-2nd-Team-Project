package com.qtai.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link DevUserIdHeaderFilter} 단위 테스트.
 *
 * <p>dev 전용 임시 인증 필터의 권한 주입(`X-Dev-Roles` → `ROLE_*`) 경로를 검증한다.
 * ROLE_ADMIN을 주입하는 보안 민감 경로이므로 다음을 보장한다:
 * 헤더 없음 시 ROLE_USER 단독, ADMIN → ROLE_ADMIN 부여, 중복 단일화, ROLE_ 접두사 이중화 방지.
 */
class DevUserIdHeaderFilterTest {

    private final DevUserIdHeaderFilter filter = new DevUserIdHeaderFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** X-Dev-User-Id(+선택 X-Dev-Roles)로 필터를 실행하고, 부여된 권한 문자열 목록을 돌려준다. */
    private List<String> runFilterAndGetAuthorities(String userId, String rolesHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (userId != null) {
            request.addHeader("X-Dev-User-Id", userId);
        }
        if (rolesHeader != null) {
            request.addHeader("X-Dev-Roles", rolesHeader);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    @DisplayName("X-Dev-Roles: ADMIN → ROLE_ADMIN + ROLE_USER 부여")
    void admin_role_header_grants_role_admin() throws Exception {
        List<String> authorities = runFilterAndGetAuthorities("1", "ADMIN");

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("X-Dev-Roles 없음 → ROLE_USER 단독")
    void no_roles_header_grants_only_role_user() throws Exception {
        List<String> authorities = runFilterAndGetAuthorities("1", null);

        assertThat(authorities).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("X-Dev-Roles: ADMIN,ADMIN → ROLE_ADMIN 단일화(중복 제거)")
    void duplicate_admin_roles_are_deduplicated() throws Exception {
        List<String> authorities = runFilterAndGetAuthorities("1", "ADMIN,ADMIN");

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(authorities).filteredOn(a -> a.equals("ROLE_ADMIN")).hasSize(1);
    }

    @Test
    @DisplayName("X-Dev-Roles: ROLE_ADMIN(접두사 포함) → ROLE_ROLE_ADMIN 미생성")
    void role_prefixed_value_is_not_double_prefixed() throws Exception {
        List<String> authorities = runFilterAndGetAuthorities("1", "ROLE_ADMIN");

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(authorities).doesNotContain("ROLE_ROLE_ADMIN");
    }

    @Test
    @DisplayName("X-Dev-User-Id 없음 → 인증 미설정")
    void no_user_id_header_sets_no_authentication() throws Exception {
        List<String> authorities = runFilterAndGetAuthorities(null, "ADMIN");

        assertThat(authorities).isEmpty();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
