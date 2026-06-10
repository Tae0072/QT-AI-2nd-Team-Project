package com.qtai.note;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * service-note 도메인 경계 검증 (CLAUDE.md §3, §4).
 *
 * <p>service-note에는 note·sharing·report가 한 프로세스에 공존하며 합법적인 in-process 의존이 있다:
 * sharing → note.api, report → sharing.api. 따라서 단순 "도메인 간 무의존(slices.notDependOnEachOther)"이
 * 아니라 <b>"다른 도메인의 internal 패키지에 직접 의존 금지"</b>를 검증한다(PR#2 개선 패턴 차용).
 *
 * <p>허용: 다른 도메인의 {@code api}/{@code api.dto} 의존(예: sharing.internal → note.api,
 * note.internal → bible.api 계약). 금지: 다른 도메인의 {@code internal} 타입 직접 참조.
 */
@AnalyzeClasses(packages = "com.qtai.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainBoundaryTest {

    @ArchTest
    static final ArchRule 타_도메인_internal_직접_의존_금지 =
            classes()
                    .that().resideInAPackage("com.qtai.domain..")
                    .should(다른_도메인_internal에_의존하지_않는다());

    private static ArchCondition<JavaClass> 다른_도메인_internal에_의존하지_않는다() {
        return new ArchCondition<>("다른 도메인의 internal 패키지에 의존하지 않는다") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String ownDomain = domainOf(clazz.getPackageName());
                if (ownDomain == null) {
                    return;
                }
                for (Dependency dependency : clazz.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String targetDomain = domainOf(targetPackage);
                    if (targetDomain != null
                            && !targetDomain.equals(ownDomain)
                            && isInternalPackage(targetPackage, targetDomain)) {
                        events.add(SimpleConditionEvent.violated(clazz,
                                clazz.getFullName() + " → " + dependency.getTargetClass().getFullName()
                                        + " : 다른 도메인 internal 직접 의존 금지"));
                    }
                }
            }
        };
    }

    /** {@code com.qtai.domain.<name>...} 패키지에서 도메인 이름 추출. 도메인 밖이면 null. */
    private static String domainOf(String packageName) {
        String prefix = "com.qtai.domain.";
        if (packageName == null || !packageName.startsWith(prefix)) {
            return null;
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    private static boolean isInternalPackage(String packageName, String domain) {
        String internalPrefix = "com.qtai.domain." + domain + ".internal";
        return packageName.equals(internalPrefix) || packageName.startsWith(internalPrefix + ".");
    }
}
