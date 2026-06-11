package com.qtai.user;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * service-user 도메인 경계 검증 (CLAUDE.md §3, §4).
 *
 * <p>service-user는 member·notification·mission을 in-process로 함께 두지만,
 * 도메인 간 호출은 반드시 상대 도메인의 {@code api} 패키지(UseCase/DTO)만 거쳐야 한다.
 * 타 도메인의 {@code internal}(Entity/Service/Repository)을 직접 참조하면 실패한다.
 * 외부 서비스(admin·note·praise·report·sharing) 의존도 {@code api} 계약 + {@code client/*Mock}만 사용한다.
 */
@AnalyzeClasses(packages = "com.qtai.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainBoundaryTest {

    /**
     * 도메인 슬라이스는 서로 의존하지 않는다 — 단, 타 도메인의 {@code api} 패키지(UseCase·DTO) 의존은 허용한다.
     * (api로 향하는 의존만 무시 → internal/web/client로 향하는 cross-domain 의존이 있으면 실패)
     */
    @ArchTest
    static final ArchRule 도메인간_의존은_api_패키지로만 =
            SlicesRuleDefinition.slices()
                    .matching("com.qtai.domain.(*)..")
                    .should().notDependOnEachOther()
                    .ignoreDependency(alwaysTrue(), resideInAPackage("..api.."));

    /** 컨트롤러(web)는 타 도메인은 물론 어떤 도메인의 internal 구현에도 직접 의존하지 않는다(api UseCase만 사용). */
    @ArchTest
    static final ArchRule web는_internal에_의존하지_않는다 =
            noClasses().that().resideInAPackage("..web..")
                    .should().dependOnClassesThat().resideInAPackage("..internal..");
}
