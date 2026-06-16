package com.qtai.domain.appversion.internal;

/**
 * 앱 업데이트 안내 강도.
 *
 * <p>사용자 앱이 실행 시 서버에서 받아 업데이트 배너 노출 방식을 결정한다.
 */
public enum AppUpdateMode {
    /** 안내 없음(최신). */
    NONE,
    /** 권장 업데이트(닫기 가능 배너). */
    RECOMMENDED,
    /** 강제 업데이트(min_supported 미만은 차단). */
    FORCED
}
