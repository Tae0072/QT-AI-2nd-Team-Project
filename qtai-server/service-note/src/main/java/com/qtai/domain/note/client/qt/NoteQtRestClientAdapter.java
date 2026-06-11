package com.qtai.domain.note.client.qt;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.ServiceCallAuthForwarder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link NoteQtClient}의 service-note 구현 — service-bible(qt 도메인) HTTP 호출 어댑터.
 *
 * <p>MSA 분리 기준(CLAUDE.md §4): qt는 service-bible 소관이라 service-note는 자체 포트 {@link NoteQtClient}로
 * "이 QT 본문을 이 회원이 읽을 수 있는가"만 확인한다. 회의록 2026-06-09 §3대로 통신은 RestClient(동기)만 사용한다.
 * 이 어댑터가 기존 {@code GetQtUseCaseMock}을 대체한다.
 *
 * <p>readability 확인은 qt의 본문 조회 엔드포인트 {@code GET /api/v1/qt/passages/{id}}를 재사용한다.
 * 존재·노출 가능하면 2xx, 없으면 404를 돌려주므로 본문 자체는 사용하지 않고 상태코드만 본다
 * ({@code toBodilessEntity}). qt는 service-bible에 있으므로 base URL은 {@code qtai.services.bible-base-url}을 쓴다.
 *
 * <p>인증(설계 §5/§81): 노트 작성/수정은 사용자 요청 맥락이라 요청의 JWT를 그대로 전달한다
 * ({@link ServiceCallAuthForwarder}). 오류 처리(CLAUDE.md §9): 광범위 catch 금지 →
 * {@link RestClientException}만 잡아 공통 예외로 감싼다(404→QT 본문 없음, 403→권한 없음, 그 외→외부 API 실패).
 */
@Component
public class NoteQtRestClientAdapter implements NoteQtClient {

    private final RestClient restClient;

    public NoteQtRestClientAdapter(RestClient.Builder restClientBuilder,
                                   ServiceEndpointsProperties endpoints) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getBibleBaseUrl()).build();
    }

    @Override
    public void validateReadable(Long memberId, Long qtPassageId) {
        if (memberId == null || qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }
        try {
            restClient.get()
                    .uri("/api/v1/qt/passages/{qtPassageId}", qtPassageId)
                    .headers(ServiceCallAuthForwarder::forward)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
                    })
                    .onStatus(status -> status.value() == 403, (request, response) -> {
                        throw new BusinessException(ErrorCode.FORBIDDEN);
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }
}
