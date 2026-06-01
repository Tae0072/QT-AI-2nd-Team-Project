package com.qtai.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_ERROR("C0001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("C0002", "올바르지 않은 요청입니다.", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION("C0003", "상태 전이를 수행할 수 없습니다.", HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND("C0004", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_IMPLEMENTED("C0005", "아직 구현되지 않은 기능입니다.", HttpStatus.NOT_IMPLEMENTED),

    // 회원
    MEMBER_NOT_FOUND("M0001", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("M0002", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("M0003", "권한이 없습니다.", HttpStatus.FORBIDDEN),
    DUPLICATE_NICKNAME("M0004", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    NICKNAME_LOCKED("M0005", "닉네임 변경 후 7일이 지나야 다시 변경할 수 있습니다.", HttpStatus.CONFLICT),
    MEMBER_ALREADY_WITHDRAWN("M0006", "이미 탈퇴한 회원입니다.", HttpStatus.CONFLICT),
    MEMBER_SUSPENDED("M0007", "정지된 회원입니다.", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN("M0008", "유효하지 않은 refresh token입니다.", HttpStatus.UNAUTHORIZED),
    KAKAO_AUTH_FAILED("M0009", "카카오 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED),

    // 알림
    NOTIFICATION_NOT_FOUND("NT0001", "알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOTIFICATION_ACCESS_DENIED("NT0002", "본인의 알림만 확인할 수 있습니다.", HttpStatus.FORBIDDEN),

    // 찬양
    PRAISE_SONG_NOT_FOUND("P0001", "찬양 곡을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PRAISE_SONG_ALREADY_SAVED("P0002", "이미 저장된 찬양입니다.", HttpStatus.CONFLICT),
    PRAISE_SONG_SAVE_NOT_FOUND("P0003", "저장된 찬양을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // AI
    AI_GENERATION_JOB_NOT_FOUND("A0001", "AI 생성 작업을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AI_ASSET_NOT_FOUND("A0002", "AI 산출물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CHECKLIST_NOT_FOUND("A0003", "AI 검증 체크리스트 버전을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_CHECKLIST_VERSION("A0004", "이미 존재하는 AI 검증 체크리스트 버전입니다.", HttpStatus.CONFLICT),
    VALIDATION_REFERENCE_JOB_NOT_FOUND("A0005", "AI 검증 참조 작업을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 성경
    BIBLE_BOOK_NOT_FOUND("B0001", "성경 책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BIBLE_VERSE_NOT_FOUND("B0002", "성경 구절을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // QT
    QT_PASSAGE_NOT_FOUND("Q0001", "QT 본문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 노트
    NOTE_NOT_FOUND("N0001", "노트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_NOTE("N0002", "이미 저장된 노트가 있습니다.", HttpStatus.CONFLICT),
    // N0003: 결번 (NOTE_BODY_REQUIRED로 정의했으나 미사용으로 삭제. NOTE_CONTENT_REQUIRED(N0005)가 본문 누락 케이스를 커버)
    NOTE_QT_PASSAGE_REQUIRED("N0004", "묵상 노트에는 QT 본문 ID가 필요합니다.", HttpStatus.BAD_REQUEST),
    NOTE_CONTENT_REQUIRED("N0005", "제목 또는 본문 중 하나를 입력해 주세요.", HttpStatus.BAD_REQUEST),
    NOTE_QT_PASSAGE_FORBIDDEN("N0006", "자유 노트에는 QT 본문 ID를 지정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    NOTE_VERSE_REQUIRED("N0007", "설교 노트에는 성경 구절이 필요합니다.", HttpStatus.BAD_REQUEST),

    // 나눔
    SHARING_POST_NOT_FOUND("S0001", "나눔 게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("S0002", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_SHARING_POST("S0003", "이미 공유된 노트입니다.", HttpStatus.CONFLICT),
    DUPLICATE_LIKE("S0004", "이미 좋아요를 누른 게시글입니다.", HttpStatus.CONFLICT),

    // 신고
    DUPLICATE_REPORT("R0001", "이미 신고한 대상입니다.", HttpStatus.CONFLICT),
    REPORT_TARGET_NOT_FOUND("R0002", "신고 대상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REPORT_NOT_FOUND("R0003", "신고를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REPORT_ALREADY_PROCESSED("R0004", "이미 처리된 신고입니다.", HttpStatus.CONFLICT),

    // 관리자
    ADMIN_USER_NOT_FOUND("AD0001", "관리자 계정을 찾을 수 없습니다.", HttpStatus.FORBIDDEN),
    ADMIN_USER_DISABLED("AD0002", "비활성화된 관리자 계정입니다.", HttpStatus.FORBIDDEN),
    ADMIN_ROLE_INSUFFICIENT("AD0003", "해당 작업에 필요한 관리자 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
