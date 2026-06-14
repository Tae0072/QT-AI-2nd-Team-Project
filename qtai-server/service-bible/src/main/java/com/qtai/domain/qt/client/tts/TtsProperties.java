package com.qtai.domain.qt.client.tts;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 외부 TTS 서버(Voice Studio / Render 배포) 호출 설정. {@code qtai.tts.*}.
 *
 * <p>QT 본문 음성을 서버에서 생성·캐시하기 위해 이 서버에 POST {readEndpoint}를 호출한다.
 * 무료 호스팅은 콜드스타트가 있으므로 타임아웃을 넉넉히 둔다. 토큰이 필요하면 env로 주입한다(로그 금지).
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "qtai.tts")
public class TtsProperties {

    /** TTS 서버 베이스 URL. */
    private String baseUrl = "https://qt-ai-2nd-team-project.onrender.com";

    /** 음성 생성 엔드포인트(POST). */
    private String readEndpoint = "/qt/read";

    /** 인증 토큰(선택). 비어 있으면 헤더를 보내지 않는다. */
    private String token = "";

    /** 기본 목소리(요청에 voice가 없을 때). */
    private String defaultVoice = "선희 (여성)";

    /** 출력 포맷. */
    private String format = "mp3";

    /** TTS 표현 파라미터(tau). */
    private double tau = 0.7;

    /** 연결 타임아웃(초) — 콜드스타트 대비. */
    private int connectTimeoutSec = 60;

    /** 응답 타임아웃(초) — 긴 본문 생성 대기. */
    private int readTimeoutSec = 150;
}
