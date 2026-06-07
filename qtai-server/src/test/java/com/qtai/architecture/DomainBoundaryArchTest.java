package com.qtai.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit 도메인 경계 검증 테스트.
 *
 * <p>CLAUDE.md §3: 도메인 간 직접 Entity/Service/Repository/infrastructure import 금지.
 * <p>CLAUDE.md §4: 패키지·레이어 기준 — api/internal/client/web 구조.
 *
 * <p>규칙 요약:
 * <ul>
 *   <li>다른 도메인의 internal, client, web 패키지를 직접 import하지 않는다</li>
 *   <li>도메인 간 통신은 반드시 api 패키지의 UseCase/DTO를 통한다</li>
 *   <li>Controller(web)는 Repository를 직접 호출하지 않는다</li>
 *   <li>common, config, security, external, batch는 공통 기술 영역으로 허용</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.qtai",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class DomainBoundaryArchTest {

    // ─── 도메인 목록 (13개) ─────────────────────────

    private static final String[] DOMAINS = {
            "admin", "ai", "audit", "bible", "member",
            "mission", "music", "note", "notification", "praise",
            "qt", "report", "sharing", "study"
    };

    // ─── 규칙 1: 다른 도메인의 internal 패키지 접근 금지 ───

    @ArchTest
    static final ArchRule admin_internal은_admin만_접근 =
            internalAccessOnlyByOwnDomain("admin");

    @ArchTest
    static final ArchRule ai_internal은_ai만_접근 =
            internalAccessOnlyByOwnDomain("ai");

    @ArchTest
    static final ArchRule audit_internal은_audit만_접근 =
            internalAccessOnlyByOwnDomain("audit");

    @ArchTest
    static final ArchRule bible_internal은_bible만_접근 =
            internalAccessOnlyByOwnDomain("bible");

    @ArchTest
    static final ArchRule member_internal은_member만_접근 =
            internalAccessOnlyByOwnDomain("member");

    @ArchTest
    static final ArchRule mission_internal은_mission만_접근 =
            internalAccessOnlyByOwnDomain("mission");

    @ArchTest
    static final ArchRule music_internal은_music만_접근 =
            internalAccessOnlyByOwnDomain("music");

    @ArchTest
    static final ArchRule note_internal은_note만_접근 =
            internalAccessOnlyByOwnDomain("note");

    @ArchTest
    static final ArchRule notification_internal은_notification만_접근 =
            internalAccessOnlyByOwnDomain("notification");

    @ArchTest
    static final ArchRule praise_internal은_praise만_접근 =
            internalAccessOnlyByOwnDomain("praise");

    @ArchTest
    static final ArchRule qt_internal은_qt만_접근 =
            internalAccessOnlyByOwnDomain("qt");

    @ArchTest
    static final ArchRule report_internal은_report만_접근 =
            internalAccessOnlyByOwnDomain("report");

    @ArchTest
    static final ArchRule sharing_internal은_sharing만_접근 =
            internalAccessOnlyByOwnDomain("sharing");

    @ArchTest
    static final ArchRule study_internal은_study만_접근 =
            internalAccessOnlyByOwnDomain("study");

    // ─── 규칙 2: 다른 도메인의 web 패키지 접근 금지 (도메인별) ──

    @ArchTest
    static final ArchRule admin_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("admin");

    @ArchTest
    static final ArchRule ai_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("ai");

    @ArchTest
    static final ArchRule audit_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("audit");

    @ArchTest
    static final ArchRule bible_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("bible");

    @ArchTest
    static final ArchRule member_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("member");

    @ArchTest
    static final ArchRule mission_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("mission");

    @ArchTest
    static final ArchRule music_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("music");

    @ArchTest
    static final ArchRule note_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("note");

    @ArchTest
    static final ArchRule notification_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("notification");

    @ArchTest
    static final ArchRule praise_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("praise");

    @ArchTest
    static final ArchRule qt_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("qt");

    @ArchTest
    static final ArchRule report_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("report");

    @ArchTest
    static final ArchRule sharing_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("sharing");

    @ArchTest
    static final ArchRule study_web은_다른_도메인_web에_의존하지_않는다 =
            webDoesNotDependOnOtherDomainWeb("study");

    // ─── 규칙 3: Controller → Repository 직접 호출 금지 ──

    @ArchTest
    static final ArchRule Controller는_Repository를_직접_호출하지_않는다 =
            noClasses()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("Repository")
                    .as("CLAUDE.md §3: Controller는 Repository를 직접 호출하지 않는다");

    // ─── 규칙 4: (삭제됨 — 13개 per-domain internal 접근 규칙이 동일 검증을 수행) ──

    // ─── 규칙 5: api 패키지는 자기 도메인 internal을 의존하지 않는다 ──

    @ArchTest
    static final ArchRule api_패키지는_같은_도메인_internal도_의존하지_않는다 =
            noClasses()
                    .that().resideInAnyPackage("..domain..api..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..domain..internal..")
                    .as("CLAUDE.md §4: api 패키지(UseCase/DTO)는 internal 구현에 의존하지 않는다 — 의존 방향은 internal → api");

    // ─── 헬퍼 메서드 ────────────────────────────────

    /**
     * 특정 도메인의 internal 패키지는 해당 도메인 소속 클래스만 접근 가능.
     * 다른 도메인의 어떤 패키지(api/internal/client/web)에서도 접근 금지.
     */
    private static ArchRule internalAccessOnlyByOwnDomain(String domain) {
        return classes()
                .that().resideInAPackage("..domain." + domain + ".internal..")
                .should().onlyBeAccessed().byAnyPackage(
                        "..domain." + domain + ".."
                )
                .as("CLAUDE.md §3: " + domain + " internal은 " + domain + " 도메인만 접근 가능");
    }

    /**
     * 특정 도메인의 web 패키지는 다른 도메인의 web 패키지에 의존하지 않는다.
     * 같은 도메인 내 web 클래스 간 참조는 허용한다.
     */
    private static ArchRule webDoesNotDependOnOtherDomainWeb(String domain) {
        String[] otherWebPackages = new String[DOMAINS.length - 1];
        int idx = 0;
        for (String d : DOMAINS) {
            if (!d.equals(domain)) {
                otherWebPackages[idx++] = "..domain." + d + ".web..";
            }
        }
        return noClasses()
                .that().resideInAPackage("..domain." + domain + ".web..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(otherWebPackages)
                .as("CLAUDE.md §3: " + domain + " web은 다른 도메인의 web 패키지에 의존하지 않는다")
                // 일부 도메인(예: mission)은 명세상 사용자 web 엔드포인트가 없어 web 클래스가 0건일 수 있다.
                // 이 경우 "의존하지 않는다" 규칙은 공허하게 참이므로 빈 집합을 허용한다.
                .allowEmptyShould(true);
    }
}
