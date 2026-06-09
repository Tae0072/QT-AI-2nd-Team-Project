package com.qtai.bible;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * service-bible 도메인 경계 검증 (CLAUDE.md §3, §4).
 *
 * <p>현재 service-bible의 도메인(bible·music·praise)은 서로 독립이어야 한다.
 * (PR #2에서 qt→bible.api 같은 합법적 의존이 생기면, api 패키지만 허용하도록 규칙을 정교화한다.)
 */
@AnalyzeClasses(packages = "com.qtai.domain", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainBoundaryTest {

    @ArchTest
    static final ArchRule 도메인은_서로_의존하지_않는다 =
            SlicesRuleDefinition.slices()
                    .matching("com.qtai.domain.(*)..")
                    .should().notDependOnEachOther();
}
