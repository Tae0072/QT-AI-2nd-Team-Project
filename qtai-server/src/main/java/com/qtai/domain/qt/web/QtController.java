package com.qtai.domain.qt.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QT REST 엔드포인트. base path: /api/v1/qt
 *
 * <p>1차 구현 엔드포인트:
 * <ul>
 *   <li>GET /today              → 오늘의 QT 본문 (GetTodayQtUseCase) — F-01</li>
 *   <li>GET /passages/{id}      → 특정 QT 본문 조회 (GetTodayQtUseCase) — F-01</li>
 * </ul>
 *
 * <p>TODO (후속 PR):
 * <ul>
 *   <li>POST   /        → 작성 (CreateQtUseCase)</li>
 *   <li>GET    /{id}    → 사용자 QT 단건 조회 (GetQtUseCase)</li>
 *   <li>GET    /my      → 내 목록 (ListMyQtUseCase)</li>
 *   <li>PUT    /{id}    → 수정 (UpdateQtUseCase)</li>
 *   <li>DELETE /{id}    → 삭제 (DeleteQtUseCase)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/qt")
@RequiredArgsConstructor
public class QtController {

    private final GetTodayQtUseCase getTodayQtUseCase;

    // TODO: CreateQtUseCase, GetQtUseCase, ListMyQtUseCase, UpdateQtUseCase, DeleteQtUseCase (후속 PR)

    /**
     * 오늘의 QT 본문 조회. (F-01)
     *
     * <p>00:00~04:00 사이에는 전일 캐시를 STALE_FALLBACK으로 제공할 수 있다.
     *
     * @param memberId 인증된 사용자 ID
     * @return 오늘의 QT 본문 + 해설 진입점 + 시뮬레이터 상태 + 캐시 상태
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayQtResponse>> getToday(
            @AuthenticationPrincipal Long memberId) {
        TodayQtResponse response = getTodayQtUseCase.getToday(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 QT 본문 조회. (F-01)
     *
     * @param memberId    인증된 사용자 ID
     * @param qtPassageId QT 본문 식별자
     * @return QT 본문 통합 응답
     */
    @GetMapping("/passages/{qtPassageId}")
    public ResponseEntity<ApiResponse<TodayQtResponse>> getPassage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long qtPassageId) {
        TodayQtResponse response = getTodayQtUseCase.getPassage(memberId, qtPassageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
