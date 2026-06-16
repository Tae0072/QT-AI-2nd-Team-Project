package com.qtai.domain.qt.client.tts;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 외부 TTS 서버 호출 어댑터. QT 본문(한글)을 음성(mp3 바이트)으로 변환한다.
 *
 * <p>POST {baseUrl}{readEndpoint} 에 {@code {text, voice, tau, format}}를 보내 audio 바이트를 받는다.
 * 무료 호스팅 콜드스타트를 고려해 타임아웃을 넉넉히 둔다. 실패는 공통 예외로 감싼다(원문/토큰 로그 금지).
 */
@Slf4j
@Component
public class TtsClient {

    private final RestClient restClient;
    private final TtsProperties props;

    public TtsClient(RestClient.Builder restClientBuilder, TtsProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(props.getConnectTimeoutSec()));
        factory.setReadTimeout(java.time.Duration.ofSeconds(props.getReadTimeoutSec()));
        this.restClient = restClientBuilder
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /** 음성 생성 결과(바이트 + MIME). */
    public record GeneratedAudio(byte[] data, String mimeType) {
    }

    /**
     * 한글 본문 텍스트를 음성으로 생성한다.
     *
     * @param text  낭독할 한글 본문(QT 절 범위)
     * @param voice 목소리 식별자(null/빈값이면 기본 목소리)
     */
    public GeneratedAudio generate(String text, String voice) {
        String useVoice = (voice == null || voice.isBlank()) ? props.getDefaultVoice() : voice;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        body.put("voice", useVoice);
        body.put("tau", props.getTau());
        body.put("format", props.getFormat());

        try {
            ResponseEntity<byte[]> response = restClient.post()
                    .uri(props.getReadEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (props.getToken() != null && !props.getToken().isBlank()) {
                            headers.setBearerAuth(props.getToken());
                        }
                    })
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .toEntity(byte[].class);

            byte[] data = response.getBody();
            if (data == null || data.length == 0) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
            }
            MediaType ct = response.getHeaders().getContentType();
            String mime = ct != null ? ct.toString() : "audio/mpeg";
            return new GeneratedAudio(data, mime);
        } catch (RestClientException e) {
            log.warn("TTS 생성 호출 실패. errorType={}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }
}
