package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetSettingsUseCase;
import com.qtai.domain.member.api.UpdateSettingsUseCase;
import com.qtai.domain.member.api.dto.SettingsResponse;
import com.qtai.domain.member.api.dto.SettingsUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 설정 서비스.
 *
 * <p>첫 조회 시 기본값으로 자동 생성 (Lazy init).
 */
@Service
@RequiredArgsConstructor
public class MemberSettingsService implements GetSettingsUseCase, UpdateSettingsUseCase {

    private final MemberSettingsRepository settingsRepository;

    @Override
    @Transactional
    public SettingsResponse getSettings(Long memberId) {
        MemberSettings settings = findOrCreate(memberId);
        return toResponse(settings);
    }

    @Override
    @Transactional
    public SettingsResponse updateSettings(Long memberId, SettingsUpdateRequest request) {
        MemberSettings settings = findOrCreate(memberId);
        settings.updateNotificationEnabled(request.notificationEnabled());
        if (request.fontSize() != null) {
            settings.updateFontSize(parseFontSize(request.fontSize()));
        }
        return toResponse(settings);
    }

    private MemberSettings findOrCreate(Long memberId) {
        return settingsRepository.findByMemberId(memberId)
                .orElseGet(() -> settingsRepository.save(MemberSettings.createDefault(memberId)));
    }

    private FontSize parseFontSize(String value) {
        try {
            return FontSize.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "fontSize는 SMALL, MEDIUM, LARGE 중 하나여야 합니다: " + value);
        }
    }

    private SettingsResponse toResponse(MemberSettings settings) {
        return new SettingsResponse(
                settings.getNotificationEnabled(),
                settings.getFontSize().name()
        );
    }
}
