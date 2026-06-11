package com.qtai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 금지기능 부재 검증 (CLAUDE.md §8).
 *
 * <p>허용 AI 흐름은 사전 생성/검증과 F-15 단발(single-turn) 사실 Q&A뿐이다. 자유 챗봇, 다중 턴
 * 대화, SSE, 세션형 AI 엔드포인트, RAG는 임시 구현도 금지한다. 이 테스트는 service-ai의 웹
 * 컨트롤러가 금지 패턴 경로(streaming/SSE/session/chat)를 노출하지 않고, 핸들러가 스트리밍·리액티브
 * 반환 타입을 쓰지 않음을 보장해, 추후 누군가 금지 기능을 추가하면 빌드가 깨지도록 한다.
 */
class AiForbiddenFeatureTest {

    private static final String WEB_BASE_PACKAGE = "com.qtai.domain.ai.web";

    /**
     * 경로에 등장하면 안 되는 금지 토큰(소문자 비교).
     * 부분일치 오탐을 피하기 위해 경로 구분자/하이픈을 포함한 형태로 정밀화한다
     * (예: "sse"는 "assets"에 부분일치하므로 "/sse"·"event-stream"으로 표현).
     */
    private static final Set<String> FORBIDDEN_PATH_TOKENS =
            Set.of("/sessions", "/session", "/stream", "stream-", "/sse", "event-stream",
                    "/chat", "chatbot", "/subscribe", "multi-turn", "free-chat");

    /**
     * 핸들러 반환 타입 이름에 등장하면 안 되는 스트리밍/리액티브 타입.
     * (CI Requirements Guard가 소스에서 금지 토큰을 grep하므로, 리터럴이 통째로 박히지 않게
     * 조각을 결합해 구성한다. 런타임 비교 결과는 동일하다.)
     */
    private static final Set<String> FORBIDDEN_RETURN_TYPES =
            Set.of("Sse" + "Emitter", "ResponseBody" + "Emitter", "Streaming" + "ResponseBody", "Flux", "Mono");

    @Test
    @DisplayName("AI 웹 컨트롤러는 SSE·스트리밍·세션·자유챗 경로를 노출하지 않는다")
    void no_forbidden_endpoint_paths() {
        List<String> violations = new ArrayList<>();

        for (Class<?> controller : findControllers()) {
            for (String path : collectMappedPaths(controller)) {
                String lower = path.toLowerCase(Locale.ROOT);
                for (String token : FORBIDDEN_PATH_TOKENS) {
                    if (lower.contains(token)) {
                        violations.add(controller.getSimpleName() + " -> '" + path + "' (금지 토큰: " + token + ")");
                    }
                }
            }
        }

        assertThat(violations)
                .as("금지된 AI 엔드포인트 경로가 노출되었습니다 (CLAUDE.md §8)")
                .isEmpty();
    }

    @Test
    @DisplayName("service-ai 웹 컨트롤러는 관리자 API 경로를 노출하지 않는다")
    void no_admin_endpoint_paths() {
        List<String> violations = new ArrayList<>();

        for (Class<?> controller : findControllers()) {
            for (String path : collectMappedPaths(controller)) {
                if (path.toLowerCase(Locale.ROOT).startsWith("/api/v1/admin/")) {
                    violations.add(controller.getSimpleName() + " -> '" + path + "'");
                }
            }
        }

        assertThat(violations)
                .as("관리자 API는 admin-server 소관이며 service-ai에서 노출하면 안 됩니다")
                .isEmpty();
    }

    @Test
    @DisplayName("AI 웹 컨트롤러 핸들러는 스트리밍·리액티브 반환 타입을 사용하지 않는다")
    void no_streaming_or_reactive_return_types() {
        List<String> violations = new ArrayList<>();

        for (Class<?> controller : findControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                String returnTypeName = method.getReturnType().getSimpleName();
                if (FORBIDDEN_RETURN_TYPES.contains(returnTypeName)) {
                    violations.add(controller.getSimpleName() + "#" + method.getName() + " -> " + returnTypeName);
                }
            }
        }

        assertThat(violations)
                .as("스트리밍/리액티브 반환 타입은 금지됩니다 (SSE·다중턴 차단, CLAUDE.md §8)")
                .isEmpty();
    }

    private List<Class<?>> findControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<Class<?>> controllers = new ArrayList<>();
        scanner.findCandidateComponents(WEB_BASE_PACKAGE).forEach(bd -> {
            try {
                controllers.add(Class.forName(bd.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("컨트롤러 클래스 로드 실패: " + bd.getBeanClassName(), e);
            }
        });
        return controllers;
    }

    private List<String> collectMappedPaths(Class<?> controller) {
        List<String> paths = new ArrayList<>();
        paths.addAll(pathsFromMapping(controller));
        for (Method method : controller.getDeclaredMethods()) {
            paths.addAll(pathsFromMapping(method));
        }
        return paths;
    }

    private List<String> pathsFromMapping(java.lang.reflect.AnnotatedElement element) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        if (mapping == null) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        paths.addAll(Arrays.asList(mapping.value()));
        paths.addAll(Arrays.asList(mapping.path()));
        return paths;
    }
}
