package com.qtai.domain.ai.web;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.generation.CreateAiGenerationJobUseCase;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobResult;

@RestController
@RequestMapping("/api/v1/system/ai/generation-jobs")
public class SystemAiGenerationJobController {

    private static final String REQUESTED_BY = "SYSTEM_BATCH";
    private static final String TARGET_TYPE_QT_PASSAGE = "QT_PASSAGE";

    private final CreateAiGenerationJobUseCase createAiGenerationJobUseCase;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SystemAiGenerationJobController(CreateAiGenerationJobUseCase createAiGenerationJobUseCase) {
        this(createAiGenerationJobUseCase, Clock.systemDefaultZone());
    }

    SystemAiGenerationJobController(CreateAiGenerationJobUseCase createAiGenerationJobUseCase, Clock clock) {
        this.createAiGenerationJobUseCase = createAiGenerationJobUseCase;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemAiGenerationJobResponse>> createGenerationJob(
            Authentication authentication,
            @Valid @RequestBody SystemAiGenerationJobRequest request
    ) {
        requireSystemBatchAuthentication(authentication);

        OffsetDateTime requestedAt = OffsetDateTime.now(clock);
        CreateAiGenerationJobResult result = createAiGenerationJobUseCase.createAiGenerationJob(
                new CreateAiGenerationJobCommand(
                        mapJobType(request.jobType()),
                        requireQtPassageTargetType(request.targetType()),
                        request.targetId(),
                        request.promptVersionId(),
                        REQUESTED_BY,
                        requestedAt
                )
        );

        return ResponseEntity.accepted().body(ApiResponse.success(new SystemAiGenerationJobResponse(
                result.generationJobId(),
                result.status(),
                requestedAt
        )));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case INVALID_STATUS_TRANSITION -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException() {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    private static void requireSystemBatchAuthentication(Authentication requestAuthentication) {
        Authentication authentication = requestAuthentication != null
                ? requestAuthentication
                : SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        if (!authorities.contains("SYSTEM_BATCH") && !authorities.contains("ROLE_SYSTEM_BATCH")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static String mapJobType(String jobType) {
        return switch (jobType) {
            case "DAILY_QT_EXPLANATION" -> "EXPLANATION";
            case "DAILY_QT_SIMULATOR" -> "SIMULATOR";
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT, "jobType is not supported");
        };
    }

    private static String requireQtPassageTargetType(String targetType) {
        if (!TARGET_TYPE_QT_PASSAGE.equals(targetType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "targetType is not supported");
        }
        return TARGET_TYPE_QT_PASSAGE;
    }
}
