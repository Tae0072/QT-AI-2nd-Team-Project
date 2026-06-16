package com.qtai.domain.appversion.internal;

/**
 * 업데이트 예정 항목 상태.
 */
public enum PendingUpdateStatus {
    /** 적용 대기(앱 재설치/출시 필요). */
    PENDING,
    /** 적용 완료(앱 출시 버전이 올라감). */
    APPLIED
}
