package com.qtai.domain.ai.api.admin.asset;

import com.qtai.domain.ai.api.admin.asset.dto.GenerateQtPassageExplanationCommand;
import com.qtai.domain.ai.api.admin.asset.dto.GenerateQtPassageExplanationResult;

/**
 * 관리자 해설 생성 트리거 UseCase (F-02/F-06/F-14).
 *
 * <p>특정 QT 본문의 미생성 해설에 대해 생성 job을 시딩하고 감사 로그를 남긴다.
 * 사용자 요청 경로가 아니라 관리자 운영 경로 전용이다(CLAUDE.md §6 — 사용자 요청 즉시 생성 금지).
 */
public interface GenerateQtPassageExplanationUseCase {

    GenerateQtPassageExplanationResult generateQtPassageExplanation(GenerateQtPassageExplanationCommand command);
}
