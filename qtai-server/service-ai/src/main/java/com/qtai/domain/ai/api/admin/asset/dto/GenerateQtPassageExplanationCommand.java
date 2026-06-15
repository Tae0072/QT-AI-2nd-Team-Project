package com.qtai.domain.ai.api.admin.asset.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 관리자 해설 생성 트리거 커맨드 (F-02/F-06).
 *
 * <p>특정 QT 본문({@code qtPassageId})의 미생성 해설에 대해 생성 job을 시딩한다.
 * 감사 로그 기록을 위해 요청 관리자({@code adminId})를 함께 전달한다.
 */
public record GenerateQtPassageExplanationCommand(
        @NotNull @Positive Long qtPassageId,
        @NotNull @Positive Long adminId,
        @NotNull OffsetDateTime requestedAt
) {
}
