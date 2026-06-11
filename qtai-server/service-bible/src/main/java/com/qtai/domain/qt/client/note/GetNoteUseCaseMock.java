package com.qtai.domain.qt.client.note;

import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * note 도메인 {@link GetNoteUseCase}의 임시 구현체(Mock).
 *
 * <p>MSA 분리 과도기 어댑터다. note 도메인은 service-bible이 아니라 service-note에서
 * 운영되므로, 통합(RestClient 어댑터) 전까지 호출자(qt) 쪽 {@code client/note}에 둔
 * 이 Mock으로 컴파일·기동을 유지한다. (CLAUDE.md §4 — 호출자 도메인 client/{타도메인명})
 *
 * <p>동작: DRAFT 노트가 "없음"({@code exists=false})으로 응답한다. {@code QtService}는
 * 이 값을 받아 {@code draftNoteId=null}로 enrich하므로, 노트 연동이 빠진 상태에서도
 * QT 본문 응답 자체는 정상 반환된다.
 *
 * <p>추후 service-note 연동 어댑터(RestClient 기반)가 진짜 {@code GetNoteUseCase} 구현으로
 * 등록되면 이 Mock 파일을 삭제한다(CLAUDE.md §4 — 진짜 구현체 등록 시 Mock 제거).
 *
 * <p>로그에는 memberId 외 민감정보를 남기지 않는다(CLAUDE.md §9).
 */
@Slf4j
@Component
public class GetNoteUseCaseMock implements GetNoteUseCase {

    @Override
    public NoteDetailResponse get(Long memberId, Long noteId) {
        // qt 도메인은 getDraft만 사용한다. 통합 전 단건 조회는 지원하지 않음을 명시.
        log.debug("[mock] note get 호출 — 통합 전 Mock은 단건 조회를 지원하지 않는다. memberId={}, noteId={}",
                memberId, noteId);
        return null;
    }

    @Override
    public NoteDraftResponse getDraft(Long memberId, NoteCategory category, Long qtPassageId) {
        log.debug("[mock] note getDraft 호출 — DRAFT 없음으로 응답. memberId={}, category={}, qtPassageId={}",
                memberId, category, qtPassageId);
        return new NoteDraftResponse(false, null);
    }
}
