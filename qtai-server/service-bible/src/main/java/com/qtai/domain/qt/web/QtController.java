package com.qtai.domain.qt.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.security.AuthenticationSupport;
import com.qtai.domain.qt.api.GetBiblePassageStudyUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.BiblePassageStudy;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
 * <p>두 엔드포인트 모두 인증 필요(SecurityConfig anyRequest().authenticated()).
 * 컨트롤러 진입부에서 {@link AuthenticationSupport#requireMemberId(Long)}로 한 번 더 방어한다.
 *
 * <p>참고: 사용자 묵상 기록(작성/수정/삭제)은 note 도메인(MEDITATION 등 /api/v1/notes)이 담당한다.
 */
@RestController
@RequestMapping("/api/v1/qt")
@RequiredArgsConstructor
public class QtController {

    private final GetTodayQtUseCase getTodayQtUseCase;
    private final GetBiblePassageStudyUseCase getBiblePassageStudyUseCase;

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
        Long authenticatedId = AuthenticationSupport.requireMemberId(memberId);
        TodayQtResponse response = getTodayQtUseCase.getToday(authenticatedId);
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
        Long authenticatedId = AuthenticationSupport.requireMemberId(memberId);
        TodayQtResponse response = getTodayQtUseCase.getPassage(authenticatedId, qtPassageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 성경 목차에서 선택한 본문 범위의 해설 진입점 가용성 조회. (F-01·F-08)
     *
     * <p>성경 본문 전체 페이지가 이 결과로 해설 버튼 활성 여부를 정한다. 매핑/해설이
     * 없으면 {@code {qtPassageId:null, hasExplanation:false}}를 반환한다(차단 아님).
     *
     * @param memberId  인증된 사용자 ID
     * @param bookCode  성경 권 코드(예: GEN)
     * @param chapter   장
     * @param verseFrom 시작 절
     * @param verseTo   끝 절
     * @return 해설 진입점 가용성(qtPassageId·hasExplanation)
     */
    @GetMapping("/passage-study")
    public ResponseEntity<ApiResponse<BiblePassageStudy>> getPassageStudy(
            @AuthenticationPrincipal Long memberId,
            @RequestParam String bookCode,
            @RequestParam int chapter,
            @RequestParam int verseFrom,
            @RequestParam int verseTo) {
        AuthenticationSupport.requireMemberId(memberId);
        BiblePassageStudy response =
                getBiblePassageStudyUseCase.getPassageStudy(bookCode, chapter, verseFrom, verseTo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
