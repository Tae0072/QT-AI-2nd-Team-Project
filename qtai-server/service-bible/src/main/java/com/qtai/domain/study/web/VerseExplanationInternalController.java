package com.qtai.domain.study.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.study.api.HidePublishedVerseExplanationUseCase;
import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.PublishApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationResult;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 승인 해설(verse explanation) 게시/숨김/조회 — 서비스 간 <b>내부 배치(SYSTEM_BATCH) 전용</b> 엔드포인트.
 *
 * <p>service-ai의 AI 자산 검수(승인→게시, 반려→숨김)·해설 시딩이 study 콘텐츠에 승인본을 반영할 때 호출한다.
 * 사용자용 학습 콘텐츠 조회({@link QtStudyContentController})와 달리, 게시/숨김은 콘텐츠 상태를 바꾸는 쓰기이고
 * 조회({@code listApprovedByVerseIds})는 AI 자산 메타(aiAssetId)를 포함하므로 일반 사용자에게 노출하지 않는다
 * (CLAUDE.md §7·§10 — 승인 게이트, 검증 참조자료 미노출). 따라서 {@code @PreAuthorize("hasRole('SYSTEM_BATCH')")}로
 * 시스템 배치 호출에만 허용한다(일반 사용자·ADMIN은 403). 시스템 토큰 검증은 {@code JwtAuthenticationFilter} HS256 폴백(PR #440).
 *
 * <p>승인본의 사용자 노출은 {@link QtStudyContentController}/study-content가 담당하며, 이 컨트롤러는 그 원천
 * (verse_explanations)을 갱신할 뿐이다.
 */
@RestController
@RequestMapping("/api/v1/study/verse-explanations")
@RequiredArgsConstructor
public class VerseExplanationInternalController {

    private final PublishApprovedVerseExplanationUseCase publishApprovedVerseExplanationUseCase;
    private final HidePublishedVerseExplanationUseCase hidePublishedVerseExplanationUseCase;
    private final ListApprovedVerseExplanationUseCase listApprovedVerseExplanationUseCase;

    /** 승인된 해설을 게시한다(같은 구절의 기존 ACTIVE 승인본은 비활성화 후 교체). */
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<PublishApprovedVerseExplanationResult> publish(
            @Valid @RequestBody PublishApprovedVerseExplanationCommand command) {
        return ApiResponse.success(
                publishApprovedVerseExplanationUseCase.publishApprovedVerseExplanation(command));
    }

    /** 특정 AI 자산으로 게시된 해설을 숨긴다(반려·회수). */
    @PostMapping("/hide")
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<HidePublishedVerseExplanationResult> hide(
            @Valid @RequestBody HidePublishedVerseExplanationCommand command) {
        return ApiResponse.success(
                hidePublishedVerseExplanationUseCase.hidePublishedVerseExplanation(command));
    }

    /** 구절 id 목록으로 ACTIVE 승인 해설을 조회한다(시딩 시 이미 게시된 구절 판별용). */
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_BATCH')")
    public ApiResponse<List<ApprovedVerseExplanationResponse>> listApproved(
            @RequestParam List<Long> verseIds) {
        return ApiResponse.success(listApprovedVerseExplanationUseCase.listApprovedByVerseIds(verseIds));
    }
}
