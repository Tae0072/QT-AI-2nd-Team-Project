package com.qtai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * lib-common-web 모듈 경계 검증 (MSA 분리 Phase 0).
 *
 * <p>servlet/JPA 공통 모듈 `lib-common-web`(com.qtai.common — GlobalExceptionHandler·BaseEntity)도
 * 코어 `lib-common`과 마찬가지로 leaf여야 한다 — 도메인이나 앱 기술영역에 의존하면 servlet
 * 서비스 추출 시 모든 것을 끌고 와 분리가 불가능해진다. 코어 경계
 * (LibCommonBoundaryArchTest)와 별개로 자기 소스셋에서 의존 방향을 기계적으로 강제한다.
 */
@AnalyzeClasses(
        packages = "com.qtai",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class LibCommonWebBoundaryArchTest {

    @ArchTest
    static final ArchRule libCommonWeb은_도메인에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("com.qtai.common..")
                    .should().dependOnClassesThat().resideInAPackage("com.qtai.domain..");

    @ArchTest
    static final ArchRule libCommonWeb은_앱_기술영역에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("com.qtai.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.qtai.config..",
                            "com.qtai.security..",
                            "com.qtai.external..",
                            "com.qtai.batch.."
                    );
}
