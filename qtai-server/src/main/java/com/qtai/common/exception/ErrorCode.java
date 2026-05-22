package com.qtai.common.exception;

/**
 * 서비스 전역 에러 카탈로그.
 *
 * 코드 규칙: 도메인 약자(1자) + 4자리 번호. C=Common, M=Member, Q=Qt, ...
 * 클라이언트는 code로 분기하고, message는 기본 사용자 안내 문구.
 */
public enum ErrorCode {

    // 공통
    INTERNAL_ERROR("C0001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT("C0002", "잘못된 요청입니다."),
    INVALID_STATUS_TRANSITION("C0003", "상태 전이를 수행할 수 없습니다."),

    // 회원
    MEMBER_NOT_FOUND("M0001", "회원을 찾을 수 없습니다."),
    UNAUTHORIZED("M0002", "인증이 필요합니다."),
    FORBIDDEN("M0003", "권한이 없습니다."),
    NICKNAME_DUPLICATE("M0004", "이미 사용 중인 닉네임입니다."),
    NICKNAME_CHANGE_LOCKED("M0005", "닉네임은 변경 후 7일 동안 다시 변경할 수 없습니다."),
    MEMBER_ALREADY_WITHDRAWN("M0006", "이미 탈퇴한 회원입니다."),

    // 알림
    NOTIFICATION_NOT_FOUND("N0001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_ACCESS_DENIED("N0002", "본인의 알림만 접근할 수 있습니다."),

    // 찬양
    PRAISE_SONG_NOT_FOUND("P0001", "찬양 곡을 찾을 수 없습니다."),
    PRAISE_SONG_ALREADY_SAVED("P0002", "이미 저장된 찬양입니다."),
    PRAISE_SONG_SAVE_NOT_FOUND("P0003", "저장된 찬양을 찾을 수 없습니다."),

    // AI
    AI_GENERATION_JOB_NOT_FOUND("A0001", "AI 생성 작업을 찾을 수 없습니다."),
    AI_ASSET_NOT_FOUND("A0002", "AI 산출물을 찾을 수 없습니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
