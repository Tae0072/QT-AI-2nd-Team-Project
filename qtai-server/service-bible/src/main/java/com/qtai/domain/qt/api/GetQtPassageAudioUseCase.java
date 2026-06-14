package com.qtai.domain.qt.api;

import com.qtai.domain.qt.api.dto.QtPassageAudioResponse;

/**
 * 오늘 QT 본문 TTS 음성 조회 UseCase 포트.
 *
 * GET /api/v1/qt/passages/{qtPassageId}/audio?voice=...
 *
 * <p>지연 캐시: (QT 본문, 목소리) 조합이 캐시에 있으면 즉시 반환, 없으면 본문(한글 절 범위)을
 * 외부 TTS 서버로 생성해 DB에 저장한 뒤 반환한다. 본문만 읽으며 노트·해설·영상·영어는 포함하지 않는다.
 */
public interface GetQtPassageAudioUseCase {

    QtPassageAudioResponse getAudio(Long qtPassageId, String voice);
}
