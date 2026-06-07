package com.qtai.domain.member.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.domain.member.api.dto.SettingsResponse;
import com.qtai.domain.member.api.dto.SettingsUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberSettingsServiceTest {

    @Mock
    private MemberSettingsRepository settingsRepository;

    @InjectMocks
    private MemberSettingsService settingsService;

    private static final Long MEMBER_ID = 1L;

    @Test
    void getSettings_기존_설정이_있으면_반환한다() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsResponse response = settingsService.getSettings(MEMBER_ID);

        assertThat(response.notificationEnabled()).isTrue();
        assertThat(response.fontSize()).isEqualTo("MEDIUM");
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void getSettings_설정이_없으면_기본값으로_생성한다() {
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());
        given(settingsRepository.save(any(MemberSettings.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SettingsResponse response = settingsService.getSettings(MEMBER_ID);

        assertThat(response.notificationEnabled()).isTrue();
        assertThat(response.fontSize()).isEqualTo("MEDIUM");
        verify(settingsRepository).save(any(MemberSettings.class));
    }

    @Test
    void updateSettings_알림만_변경한다() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsUpdateRequest request = new SettingsUpdateRequest(false, null, null, null, null);
        SettingsResponse response = settingsService.updateSettings(MEMBER_ID, request);

        assertThat(response.notificationEnabled()).isFalse();
        assertThat(response.fontSize()).isEqualTo("MEDIUM");
    }

    @Test
    void updateSettings_폰트만_변경한다() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsUpdateRequest request = new SettingsUpdateRequest(null, "LARGE", null, null, null);
        SettingsResponse response = settingsService.updateSettings(MEMBER_ID, request);

        assertThat(response.notificationEnabled()).isTrue();
        assertThat(response.fontSize()).isEqualTo("LARGE");
    }

    @Test
    void updateSettings_둘_다_변경한다() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsUpdateRequest request = new SettingsUpdateRequest(false, "SMALL", null, null, null);
        SettingsResponse response = settingsService.updateSettings(MEMBER_ID, request);

        assertThat(response.notificationEnabled()).isFalse();
        assertThat(response.fontSize()).isEqualTo("SMALL");
    }

    @Test
    void updateSettings_잘못된_fontSize_시_BusinessException() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsUpdateRequest request = new SettingsUpdateRequest(null, "INVALID", null, null, null);

        assertThatThrownBy(() -> settingsService.updateSettings(MEMBER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fontSize");
    }

    @Test
    void updateSettings_소문자_fontSize도_처리한다() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsUpdateRequest request = new SettingsUpdateRequest(null, "small", null, null, null);
        SettingsResponse response = settingsService.updateSettings(MEMBER_ID, request);

        assertThat(response.fontSize()).isEqualTo("SMALL");
    }

    @Test
    void updateSettings_음악_설정을_변경한다() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsUpdateRequest request =
                new SettingsUpdateRequest(null, null, false, 30, "HYMN");
        SettingsResponse response = settingsService.updateSettings(MEMBER_ID, request);

        assertThat(response.musicEnabled()).isFalse();
        assertThat(response.musicVolume()).isEqualTo(30);
        assertThat(response.musicCategory()).isEqualTo("HYMN");
    }

    @Test
    void updateSettings_음악_볼륨은_0_100_범위로_보정된다() {
        MemberSettings existing = MemberSettings.createDefault(MEMBER_ID);
        given(settingsRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(existing));

        SettingsResponse over = settingsService.updateSettings(
                MEMBER_ID, new SettingsUpdateRequest(null, null, null, 150, null));
        assertThat(over.musicVolume()).isEqualTo(100);

        SettingsResponse under = settingsService.updateSettings(
                MEMBER_ID, new SettingsUpdateRequest(null, null, null, -10, null));
        assertThat(under.musicVolume()).isEqualTo(0);
    }

    @Test
    void getSettings_음악_기본값은_ON_70_BGM() {
        given(settingsRepository.findByMemberId(MEMBER_ID))
                .willReturn(Optional.of(MemberSettings.createDefault(MEMBER_ID)));

        SettingsResponse response = settingsService.getSettings(MEMBER_ID);

        assertThat(response.musicEnabled()).isTrue();
        assertThat(response.musicVolume()).isEqualTo(70);
        assertThat(response.musicCategory()).isEqualTo("BGM");
    }
}
