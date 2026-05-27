# 2026-05-26 TTS 음성 클로닝 엔진 탐색 및 GPT-SoVITS 도입 결과 보고

## 요약
Bark TTS의 낮은 목소리 복제 품질을 개선하기 위해 6개 엔진을 비교 평가했다.
F5-TTS, CosyVoice는 환경 문제로 탈락하고, XTTS v2는 품질 부족,
Edge TTS/OpenVoice는 복제 불가로 제외했다.
최종적으로 GPT-SoVITS v2pro를 선정하여 HTTP API 방식으로 통합했다.
팀원 3명의 목소리를 녹음하여 클로닝 테스트를 완료했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `bible-tts/gpt_sovits_engine.py` | GPT-SoVITS API 서버 래퍼 (335줄) |
| `bible-tts/tts_app.py` | 다중 엔진 지원으로 리팩토링된 GUI 앱 |
| `bible-tts/voices/00_recording.wav` | Lead 강태오 참조 음성 |
| `bible-tts/voices/이승욱 1_recording.wav` | 이승욱 참조 음성 |
| `bible-tts/voices/김지민  ㅠ_recording.m4a` | 김지민 참조 음성 |

## 아키텍처 비교

### 엔진 비교 결과

| 엔진 | 한국어 | 복제 품질 | VRAM | Windows | 결과 |
|------|--------|----------|------|---------|------|
| F5-TTS | O | 높음 | >10GB | 의존성 충돌 | ❌ 탈락 |
| XTTS v2 | △ | 중간 | ~4GB | O | △ 예비 |
| CosyVoice | O | 높음 | ~6GB | Linux 전용 | ❌ 탈락 |
| Edge TTS | O | 복제불가 | 0 | O | 폴백용 |
| OpenVoice | △ | 톤 변환만 | ~2GB | O | ❌ 탈락 |
| GPT-SoVITS | O | 매우 높음 | ~3.4GB | O | ✅ 선정 |

### 아키텍처 변경

### 이전 (05-22)
```
tts_app.py → bark_engine.py (직접 호출)
           → edge_engine.py (직접 호출)
```

### 이후 (05-26)
```
tts_app.py → gpt_sovits_engine.py → HTTP API → GPT-SoVITS 서버 (별도 프로세스)
           → bark_engine.py (폴백)
           → edge_engine.py (폴백)
```

## 구현 상세

### GPT-SoVITS 엔진 래퍼 (`gpt_sovits_engine.py`)
GPT-SoVITS는 독립된 HTTP API 서버로 구동된다 (포트 9880).
`gpt_sovits_engine.py`는 이 서버에 HTTP 요청을 보내는 클라이언트 모듈이다.

주요 함수:
- `check_server()`: GET 요청으로 서버 상태 확인
- `_prepare_ref_audio()`: ffmpeg로 참조 음성을 3~8초 WAV로 변환/트리밍
- `generate_speech()`: POST `/tts`로 텍스트 → 음성 생성 요청
- `generate_speech_long()`: 긴 텍스트 처리 (서버 자체 분할 `text_split_method` 활용)

API 요청 파라미터:
```python
payload = {
    "text": clean_text,
    "text_lang": "ko",
    "ref_audio_path": ref_audio_path,
    "prompt_text": prompt_text,
    "prompt_lang": "ko",
    "speed_factor": 1.0,
    "repetition_penalty": 1.35,
    "top_k": 5,
    "temperature": 1.0,
}
```

### 참조 음성 처리
GPT-SoVITS는 3~10초 길이의 참조 음성을 요구한다.
녹음 원본이 10초를 넘으면 앞부분 8초를 잘라서 사용하고,
WAV가 아닌 포맷(m4a 등)은 ffmpeg로 16kHz mono WAV로 변환한다.
이미 처리된 파일은 `{이름}_ref_trim.wav`로 캐싱하여 재사용한다.

### 팀원 목소리 녹음
앱의 녹음 기능을 사용하여 3명의 팀원 목소리를 수집했다.
각 녹음은 약 10초 분량이며, `voices/` 디렉토리에 저장되었다.
GPT-SoVITS 기본 모델(pretrained)만으로도 목소리 유사도가
이전 Bark 대비 크게 향상되었다.

## 발생한 문제와 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| F5-TTS 설치 실패 | PyTorch 버전 충돌 + VRAM 부족 | 후보에서 제외 |
| CosyVoice Linux 전용 | `torchaudio` 백엔드 Sox가 Windows 미지원 | 후보에서 제외 |
| GPT-SoVITS 서버 포트 충돌 | 기본 포트 9880 이미 사용 중 | 기존 프로세스 종료 후 재시작 |
| m4a 참조 음성 인식 안 됨 | GPT-SoVITS가 WAV만 지원 | ffmpeg로 WAV 변환 후 전달 |
| 참조 음성 너무 길면 품질 저하 | 10초 초과 시 모델 혼란 | 자동 8초 트리밍 구현 |

## 검증
- [x] GPT-SoVITS 서버 `check_server()` 정상 동작
- [x] 3명 팀원 목소리 각각 음성 생성 성공
- [x] 참조 음성 자동 트리밍 동작 확인
- [x] Bark/Edge TTS 폴백 경로 여전히 정상
- [x] 앱 UI에서 엔진 선택 후 생성 흐름 정상

## 한계 및 향후 과제
- **기본 모델 한계**: pretrained 모델만으로는 개인 특성 완전 재현 어려움
- **파인튜닝 필요**: 개인별 10~30문장 녹음 후 SoVITS + GPT 파인튜닝으로 품질 향상 가능
- **서버 별도 실행 필요**: `start_gpt_sovits.bat`을 미리 실행해야 함
- **향후**: 파인튜닝 자동화 파이프라인 구축, 녹음 가이드 문장 추가
