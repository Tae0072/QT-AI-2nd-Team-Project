package com.qtai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin-server 컨트롤러 노출면 검증 (회의록 2026-06-09 §6: admin API만 남기고 나머지 삭제).
 *
 * <p>모놀리식을 통째 복사한 뒤 사용자용·system 컨트롤러를 삭제했으므로, 남은 HTTP 컨트롤러는
 * 전부 {@code /api/v1/admin/**}이어야 한다. 누군가 사용자/시스템 컨트롤러를 되살리면 이 테스트가 깨진다.
 */
class AdminControllerSurfaceTest {

    @Test
    @DisplayName("노출된 모든 @RestController/@Controller 매핑은 /api/v1/admin 하위다")
    void all_controllers_are_admin_scoped() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<String> violations = new ArrayList<>();
        scanner.findCandidateComponents("com.qtai").forEach(bd -> {
            try {
                Class<?> controller = Class.forName(bd.getBeanClassName());
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
                List<String> paths = new ArrayList<>();
                if (mapping != null) {
                    paths.addAll(Arrays.asList(mapping.value()));
                    paths.addAll(Arrays.asList(mapping.path()));
                }
                boolean adminScoped = !paths.isEmpty()
                        && paths.stream().allMatch(p -> p.startsWith("/api/v1/admin"));
                if (!adminScoped) {
                    violations.add(controller.getSimpleName() + " -> " + paths);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(violations)
                .as("admin 외 컨트롤러가 노출되어 있습니다 (admin-server는 /api/v1/admin만 노출)")
                .isEmpty();
    }
}
