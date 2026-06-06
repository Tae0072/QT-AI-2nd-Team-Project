package com.qtai.domain.study.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.domain.study.api.GetQtSimulatorUseCase;
import com.qtai.domain.study.api.dto.QtSimulatorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시뮬레이터 상태 결정 풀 컨텍스트 통합 테스트 (3단계 E2E, F-12 / CLAUDE.md §6).
 *
 * <p>기존 {@code QtSimulatorServiceTest}는 mock 단위라 실제 빈 와이어링·DB·ObjectMapper를
 * 관통하지 않는다. 이 테스트는 전체 ApplicationContext에서 {@link GetQtSimulatorUseCase}를
 * 호출해, study→qt 크로스도메인 조회 + 실제 JSON 파싱을 거쳐 상태가 결정되는지 검증한다.
 *
 * <p>핵심 계약(§6 "READY일 때만 버튼 활성화"의 서버측 근거): <b>READY 응답만 재생용 클립
 * 데이터(clipId, sceneScriptJson)를 노출하고, MISSING/FAILED는 데이터를 비운다.</b>
 *
 * <p>시드는 {@code @Sql}로 처리(엔티티 크로스패키지 생성 회피). 케이스마다 다른 키(passage/clip)를
 * 사용하고 {@code @Transactional}로 롤백해 격리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimulatorStatusFlowIntegrationTest {

    @Autowired
    private GetQtSimulatorUseCase getQtSimulatorUseCase;

    @Test
    @Sql(statements = {
        "INSERT INTO qt_passages (id, qt_date, book_id, chapter, start_verse, end_verse, title, created_at, updated_at)"
            + " VALUES (9001, '2026-05-20', 1, 1, 1, 5, '창세기 1장', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        "INSERT INTO simulator_component_library_versions (id, version, status, created_at, updated_at)"
            + " VALUES (8001, 'sim-e2e-ready', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        "INSERT INTO simulator_clips (id, qt_passage_id, title, component_library_version_id, scene_script_json, status, approved_at, created_at, updated_at)"
            + " VALUES (7001, 9001, '창세기 장면', 8001, '{\"scenes\":[]}', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void 승인_클립이_있으면_READY와_재생데이터를_노출한다() {
        QtSimulatorResponse res = getQtSimulatorUseCase.getSimulator(9001L);

        assertThat(res.status()).isEqualTo("READY");
        assertThat(res.clipId()).isEqualTo(7001L);
        assertThat(res.qtPassageId()).isEqualTo(9001L);
        assertThat(res.sceneScriptJson()).isNotNull();           // READY만 재생 데이터 노출
        assertThat(res.componentLibraryVersion()).isEqualTo("sim-e2e-ready");
        assertThat(res.clipStatus()).isEqualTo("APPROVED");
    }

    @Test
    @Sql(statements =
        "INSERT INTO qt_passages (id, qt_date, book_id, chapter, start_verse, end_verse, title, created_at, updated_at)"
            + " VALUES (9002, '2026-05-21', 1, 1, 1, 5, '본문만 있고 클립 없음', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
    void 승인_클립이_없으면_MISSING이고_재생데이터가_비어있다() {
        QtSimulatorResponse res = getQtSimulatorUseCase.getSimulator(9002L);

        assertThat(res.status()).isEqualTo("MISSING");
        assertThat(res.clipId()).isNull();
        assertThat(res.sceneScriptJson()).isNull();              // 버튼 비활성화 근거
    }

    @Test
    @Sql(statements = {
        "INSERT INTO qt_passages (id, qt_date, book_id, chapter, start_verse, end_verse, title, created_at, updated_at)"
            + " VALUES (9003, '2026-05-22', 1, 1, 1, 5, '깨진 클립', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        "INSERT INTO simulator_component_library_versions (id, version, status, created_at, updated_at)"
            + " VALUES (8003, 'sim-e2e-failed', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        "INSERT INTO simulator_clips (id, qt_passage_id, title, component_library_version_id, scene_script_json, status, approved_at, created_at, updated_at)"
            + " VALUES (7003, 9003, '깨진 장면', 8003, '{broken-json', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void 승인_클립의_JSON이_깨지면_FAILED이고_재생데이터가_비어있다() {
        QtSimulatorResponse res = getQtSimulatorUseCase.getSimulator(9003L);

        assertThat(res.status()).isEqualTo("FAILED");
        assertThat(res.clipId()).isNull();
        assertThat(res.sceneScriptJson()).isNull();              // 깨진 산출물은 사용자에게 노출하지 않음
    }
}
