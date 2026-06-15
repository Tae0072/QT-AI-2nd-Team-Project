package com.qtai.domain.ai.web;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.admin.asset.GenerateQtPassageExplanationUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.GenerateQtPassageExplanationCommand;
import com.qtai.domain.ai.api.admin.asset.dto.GenerateQtPassageExplanationResult;

/**
 * 관리자 QT 본문 해설 생성 트리거 컨트롤러 (AD-01/AD-03, F-02/F-06/F-14).
 *
 * <p>{@code POST /api/v1/admin/ai/qt-passages/{qtPassageId}/explanations/generate} —
 * 미생성 해설을 가진 QT 본문에 대해 관리자가 직접 해설 생성 job을 시딩한다.
 * 생성은 배치/시스템 처리라 즉시 완료가 아니므로 {@code 202 Accepted}로 응답한다.
 * 권한은 REVIEWER/SUPER_ADMIN(=requireReviewer). 사용자 요청 경로가 아니다(CLAUDE.md §6).
 */
@RestController
@RequestMapping("/api/v1/admin/ai/qt-passages")
public class AdminAiExplanationController {

    private final GenerateQtPassageExplanationUseCase generateQtPassageExplanationUseCase;
    private final AdminAiAuthentication adminAiAuthentication;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminAiExplanationController(
            GenerateQtPassageExplanationUseCase generateQtPassageExplanationUseCase,
            AdminAiAuthentication adminAiAuthentication
    ) {
        this(generateQtPassageExplanationUseCase, adminAiAuthentication, Clock.systemDefaultZone());
    }

    AdminAiExplanationController(
            GenerateQtPassageExplanationUseCase generateQtPassageExplanationUseCase,
            AdminAiAuthentication adminAiAuthentication,
            Clock clock
    ) {
        this.generateQtPassageExplanationUseCase = generateQtPassageExplanationUseCase;
        this.adminAiAuthentication = adminAiAuthentication;
        this.clock = clock;
    }

    @PostMapping("/{qtPassageId}/explanations/generate")
    public ResponseEntity<ApiResponse<GenerateQtPassageExplanationResult>> generate(
            @PathVariable("qtPassageId") Long qtPassageId,
            Authentication authentication
    ) {
        AdminAiAuthentication.AdminAiPrincipal adminAuthentication =
                adminAiAuthentication.requireReviewer(authentication);
        GenerateQtPassageExplanationResult result = generateQtPassageExplanationUseCase
                .generateQtPassageExplanation(new GenerateQtPassageExplanationCommand(
                        qtPassageId,
                        adminAuthentication.adminId(),
                        OffsetDateTime.now(clock)
                ));

        return ResponseEntity.accepted().body(ApiResponse.success(result));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return AiWebExceptionResponses.business(exception);
    }
}
