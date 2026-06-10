package com.qtai.ai;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * service-ai 도메인 경계 검증 (CLAUDE.md §3, §4).
 *
 * <p>ai 도메인은 다른 도메인(audit·study·qt·bible·admin)을 호출할 때 반드시 상대 도메인의
 * {@code api} 계약(UseCase/DTO)만 사용해야 하며, 상대 도메인의 {@code internal}(Entity·Service·
 * Repository 등)을 직접 import해서는 안 된다. MSA 분리 후 다른 도메인의 internal은 애초에 이 모듈에
 * 없으므로, 이 규칙은 "통합 시 RestClient 어댑터로 교체"하는 동안에도 경계가 무너지지 않도록 고정한다.
 */
@AnalyzeClasses(packages = "com.qtai", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainBoundaryTest {

    @ArchTest
    static final ArchRule ai는_타_도메인_internal에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("com.qtai.domain.ai..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.qtai.domain.audit.internal..",
                            "com.qtai.domain.study.internal..",
                            "com.qtai.domain.qt.internal..",
                            "com.qtai.domain.bible.internal..",
                            "com.qtai.domain.admin.internal..")
                    .because("다른 도메인 호출은 api 계약(UseCase/DTO)만 사용해야 한다 (internal 직접 참조 금지)");

    @ArchTest
    static final ArchRule 반입된_타_도메인_api는_internal을_끌어오지_않는다 =
            noClasses()
                    .that().resideInAnyPackage(
                            "com.qtai.domain.audit.api..",
                            "com.qtai.domain.study.api..",
                            "com.qtai.domain.qt.api..",
                            "com.qtai.domain.bible.api..",
                            "com.qtai.domain.admin.api..")
                    .should().dependOnClassesThat().resideInAPackage("com.qtai.domain..internal..")
                    .because("api 계약은 자기완결이어야 하며 어떤 도메인의 internal에도 의존하면 안 된다");
}
