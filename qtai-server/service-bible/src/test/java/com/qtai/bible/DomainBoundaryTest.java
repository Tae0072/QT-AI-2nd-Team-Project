package com.qtai.bible;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * service-bible 도메인 경계 검증 (CLAUDE.md §3, §4).
 *
 * <p>PR #2에서 qt→bible.api, study→qt.api 같은 합법적 도메인 간 의존이 생겼다. 따라서
 * "도메인끼리 전혀 의존하지 않는다"는 규칙은 더 이상 맞지 않는다. 대신 CLAUDE.md §3의 핵심
 * 불변식 — "다른 도메인의 internal(Entity/Service/Repository) 타입을 직접 import하지 않는다"
 * — 만 강제한다. 도메인 간 호출은 반드시 상대 도메인의 {@code api}(UseCase/DTO)를 거쳐야 한다.
 */
@AnalyzeClasses(packages = "com.qtai.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainBoundaryTest {

    @ArchTest
    static final ArchRule 다른_도메인의_internal에_의존하지_않는다 =
            classes()
                    .that().resideInAPackage("com.qtai.domain..")
                    .should(다른_도메인_internal_의존_없음());

    private static ArchCondition<JavaClass> 다른_도메인_internal_의존_없음() {
        return new ArchCondition<>("다른 도메인의 internal 패키지에 의존하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String fromDomain = domainOf(clazz.getPackageName());
                if (fromDomain == null) {
                    return;
                }
                for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String toDomain = domainOf(targetPackage);
                    boolean targetIsInternal = targetPackage.contains(".internal");
                    if (targetIsInternal && toDomain != null && !toDomain.equals(fromDomain)) {
                        events.add(SimpleConditionEvent.violated(
                                clazz,
                                clazz.getName() + " → " + dependency.getTargetClass().getName()
                                        + " (도메인 '" + fromDomain + "'이 도메인 '" + toDomain
                                        + "'의 internal에 의존)"));
                    }
                }
            }
        };
    }

    /** {@code com.qtai.domain.<도메인>...} 패키지명에서 도메인 세그먼트를 추출한다. */
    private static String domainOf(String packageName) {
        String prefix = "com.qtai.domain.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
