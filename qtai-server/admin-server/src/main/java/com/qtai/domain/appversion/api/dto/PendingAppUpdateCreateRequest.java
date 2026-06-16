package com.qtai.domain.appversion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 업데이트 예정 항목 등록 요청.
 *
 * @param title            제목(필수)
 * @param description      설명(선택)
 * @param targetAppVersion 적용 시 올라갈 앱 출시 버전(예: 0.2.0)
 * @param updateMode       적용 후 안내 강도. NONE|RECOMMENDED|FORCED (미지정 시 RECOMMENDED)
 */
public record PendingAppUpdateCreateRequest(
        @NotBlank(message = "제목을 입력하세요.")
        @Size(max = 150, message = "title은 150자 이하여야 합니다.")
        String title,

        @Size(max = 1000, message = "description은 1000자 이하여야 합니다.")
        String description,

        @NotBlank(message = "대상 앱 버전을 입력하세요.")
        @Size(max = 40, message = "targetAppVersion은 40자 이하여야 합니다.")
        @Pattern(regexp = "\\d+(\\.\\d+)*", message = "버전 형식은 숫자와 점(.)만 허용됩니다. 예: 0.2.0")
        String targetAppVersion,

        @Pattern(regexp = "NONE|RECOMMENDED|FORCED",
                message = "updateMode는 NONE, RECOMMENDED, FORCED만 허용됩니다.")
        String updateMode
) {
}
