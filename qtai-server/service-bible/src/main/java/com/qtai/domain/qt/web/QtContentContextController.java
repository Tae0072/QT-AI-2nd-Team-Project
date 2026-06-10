package com.qtai.domain.qt.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * QT 본문 콘텐츠 컨텍스트 조회 — 서비스 간 <b>내부 배치(SYSTEM_BATCH) 전용</b> 엔드포인트.
 *
 * <p>service-ai의 해설 생성 배치·00:05 시딩이 QT 본문의 verseId·제목·공개여부를 끌어올 때 호출한다.
 * 사용자용 {@link QtController}(/today, /passages/{id})와 달리, 이 컨텍스트는 선등록된 <b>미공개</b>
 * 본문({@code published=false})도 반환하므로(사전 생성용) 일반 사용자에게 노출하면 안 된다(CLAUDE.md §6·§8).
 * 따라서 {@code @PreAuthorize("hasRole('SYSTEM_BATCH')")}로 시스템 배치 호출에만 허용한다
 * (일반 인증 사용자·ADMIN은 403). 시스템 토큰 검증은 {@code JwtAuthenticationFilter}의 HS256 폴백(PR #440).
 */
@RestController
@RequestMapping("/api/v1/qt")
@RequiredArgsConstructor
public class QtContentContextController {

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;

    /** 본문 id로 콘텐츠 컨텍스트 조회. 없으면 {@code QT_PASSAGE_NOT_FOUND}(404). */
    @GetMapping("/passages/{qtPassageId}/content-context")
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<QtPassageContentContext> getContentContext(@PathVariable Long qtPassageId) {
        return ApiResponse.success(getQtPassageContentContextUseCase.getContentContext(qtPassageId));
    }

    /**
     * QT 날짜로 콘텐츠 컨텍스트 조회 — 내부 배치 전용(노출 정책·STALE_FALLBACK 우회, CLAUDE.md §6).
     *
     * <p>해당 날짜 본문이 없으면 {@code QT_PASSAGE_NOT_FOUND}(404)를 돌려준다. 호출자(어댑터)는
     * 404를 {@code Optional.empty()}로 변환해 원래 {@code findContentContextByDate}의 의미를 보존한다.
     */
    @GetMapping("/content-context")
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<QtPassageContentContext> getContentContextByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate qtDate) {
        return getQtPassageContentContextUseCase.findContentContextByDate(qtDate)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND));
    }
}
