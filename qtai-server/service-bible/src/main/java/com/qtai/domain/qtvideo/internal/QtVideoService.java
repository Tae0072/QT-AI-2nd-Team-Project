package com.qtai.domain.qtvideo.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qtvideo.api.GetQtVideoUseCase;
import com.qtai.domain.qtvideo.api.dto.QtVideoClipResponse;
import com.qtai.domain.qtvideo.api.dto.QtVideoUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QtVideoService implements GetQtVideoUseCase {

    // [임시 2026-06-19] 오늘 QT 영상 미생성 한시적 폴백의 '오늘(KST)' 판정용. 원복 시 제거.
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final QtVideoClipRepository qtVideoClipRepository;
    // [임시 2026-06-19] 오늘 판정용 시계(@RequiredArgsConstructor 생성자 인자로 추가됨). 원복 시 제거.
    private final Clock clock;

    @Override
    public QtVideoClipResponse getVideo(Long qtPassageId) {
        validateQtPassageId(qtPassageId);
        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(qtPassageId);
        if (!context.published()) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }

        var candidates = qtVideoClipRepository.findByQtPassageIdAndStatusInAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(
                qtPassageId,
                QtVideoUserStatusResolver.USER_STATUS_CANDIDATE_STATUSES);
        return QtVideoUserStatusResolver.chooseUserStatusClip(candidates)
                .map(this::toResponse)
                // ===== [임시 2026-06-19] 오늘 QT 영상 미생성 한시적 폴백 (F-12) =====
                // 오늘(KST) 본문에 노출 가능한 클립이 전혀 없을 때만, 가장 최근 등록(APPROVED) 영상으로
                // 한시적 대체 노출한다. 영구 로직 변경이 아니다. 원복: 이 .or(...) 한 줄 + 아래
                // recentApprovedFallbackForToday(...) + 리포지토리 findTopByStatus... + 시계(clock)/테스트 제거.
                .or(() -> recentApprovedFallbackForToday(context))
                // ===== [임시 끝] =====
                .orElseGet(() -> QtVideoClipResponse.missing(qtPassageId));
    }

    // [임시 2026-06-19] 오늘 본문에 한해 최근 등록 영상으로 폴백. 영구 로직 아님 — 원복 시 메서드 전체 제거.
    private Optional<QtVideoClipResponse> recentApprovedFallbackForToday(QtPassageContentContext context) {
        if (context.qtDate() == null || !context.qtDate().equals(LocalDate.now(clock.withZone(KST)))) {
            return Optional.empty();
        }
        return qtVideoClipRepository
                .findTopByStatusAndDeletedAtIsNullOrderByApprovedAtDescIdDesc(QtVideoClipStatus.APPROVED)
                .map(this::toResponse);
    }

    private static void validateQtPassageId(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private QtVideoClipResponse toResponse(QtVideoClip clip) {
        QtVideoUserStatus status = QtVideoUserStatusResolver.toUserStatus(clip.getStatus());
        if (status != QtVideoUserStatus.READY) {
            return QtVideoClipResponse.unavailable(
                    clip.getQtPassageId(),
                    status,
                    clip.getStatus().name()
            );
        }
        return new QtVideoClipResponse(
                status.name(),
                clip.getId(),
                clip.getQtPassageId(),
                clip.getTitle(),
                clip.getVideoUrl(),
                clip.getSourceVideo().getId(),
                clip.getStartTimeSec(),
                clip.getEndTimeSec(),
                clip.getCompositionType().name(),
                clip.getStatus().name()
        );
    }
}
