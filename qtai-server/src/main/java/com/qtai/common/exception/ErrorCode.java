package com.qtai.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 상수.
 *
 * <p>도메인별 NOT_FOUND 코드(M0001, B0001, N0001 등)와 공통 RESOURCE_NOT_FOUND(C0004)가
 * 공존한다. 도메인 코드가 존재하면 도메인 코드를 우선 사용하고,
 * 공통 코드는 도메인을 특정할 수 없는 범용 상황에서만 사용한다.
 */
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

    // AI
    AI_GENERATION_JOB_NOT_FOUND("A0001", "AI 생성 작업을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AI_ASSET_NOT_FOUND("A0002", "AI 산출물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 성경
    BIBLE_BOOK_NOT_FOUND("B0001", "성경 책을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BIBLE_VERSE_NOT_FOUND("B0002", "성경 구절을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // QT
    QT_PASSAGE_NOT_FOUND("Q0001", "QT 본문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 노트
    NOTE_NOT_FOUND("N0001", "노트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 나눔
    SHARING_POST_NOT_FOUND("S0001", "나눔 게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("S0002", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
