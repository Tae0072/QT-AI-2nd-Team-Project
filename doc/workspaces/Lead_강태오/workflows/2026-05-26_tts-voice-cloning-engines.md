# 2026-05-26 TTS 음성 클로닝 엔진 탐색 및 GPT-SoVITS 도입

## 목표
Bark TTS의 목소리 복제 품질이 낮아 "낭독" 수준에 그치는 문제를 해결하기 위해
여러 음성 클로닝 엔진을 비교 평가하고, 최적의 엔진을 선정하여 통합한다.
팀원 3명(Lead 강태오, 이승욱, 김지민)의 목소리를 녹음하여 실제 클로닝 테스트를 진행한다.

## 범위
- **엔진 후보 탐색**: F5-TTS, XTTS v2, CosyVoice, Edge TTS, OpenVoice, GPT-SoVITS
- **엔진별 설치/실행 테스트**: 3060 Ti 8GB 환경에서 실행 가능 여부 확인
- **팀원 목소리 녹음**: 3명 참조 음성 수집
- **GPT-SoVITS 선정 및 통합**: API 서버 방식으로 `gpt_sovits_engine.py` 작성
- **앱 리팩토링**: 다중 엔진 지원 구조로 `tts_app.py` 개편

## 단계

1. **엔진 후보 조사 및 비교**
   - 한국어 지원, 음성 클로닝 품질, VRAM 요구량, 라이선스 비교
   - 6개 후보 엔진 리스트업

2. **F5-TTS 테스트 (실패)**
   - GitHub 클론 및 설치 시도
   - 의존성 충돌 + VRAM 부족으로 3060 Ti에서 실행 불가
   - 후보에서 제외

3. **XTTS v2 테스트 (부분 성공)**
   - Coqui TTS의 XTTS v2 설치 및 테스트
   - 한국어 음성 생성은 되나 발음 품질이 낮음
   - 목소리 복제 유사도 중간 수준

4. **CosyVoice 테스트 (실패)**
   - Alibaba의 CosyVoice 설치 시도
   - Linux 전용 의존성 문제로 Windows에서 실행 불가

5. **Edge TTS / OpenVoice 평가**
   - Edge TTS: 높은 음질이나 목소리 복제 불가 (고정 음성만)
   - OpenVoice: 톤 변환만 가능, 실제 클로닝은 아님

6. **GPT-SoVITS v2pro 선정 및 설치**
   - HTTP API 서버 방식 → 별도 프로세스로 구동, 메모리 분리
   - `start_gpt_sovits.bat`으로 서버 실행 → `http://127.0.0.1:9880`
   - 참조 음성 3~10초만으로 높은 유사도의 목소리 복제 가능
   - 한국어 발음 품질이 후보 중 가장 우수

7. **GPT-SoVITS 엔진 래퍼 작성 (`gpt_sovits_engine.py`)**
   - `check_server()`: API 서버 상태 확인
   - `generate_speech()`: 텍스트 → 음성 WAV 생성 (POST `/tts`)
   - `_prepare_ref_audio()`: 참조 음성 3~8초 자동 트리밍
   - `generate_speech_long()`: 긴 텍스트 처리 (서버 자체 분할)

8. **팀원 목소리 녹음**
   - Lead 강태오: `00_recording.wav` (약 10초 녹음)
   - 이승욱: `이승욱 1_recording.wav` (약 10초 녹음)
   - 김지민: `김지민  ㅠ_recording.m4a` (약 10초 녹음)
   - 각 녹음을 `voices/` 디렉토리에 저장

9. **앱 리팩토링 (`tts_app.py`)**
   - 엔진 선택 UI: Bark / Edge TTS / GPT-SoVITS 드롭다운
   - GPT-SoVITS 선택 시 서버 상태 자동 확인
   - 목소리별 참조 음성 자동 매칭
   - 엔진별 설정 옵션 분기

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| TTS (선정) | GPT-SoVITS v2pro | HTTP API 서버, 한국어 최고 품질 |
| TTS (폴백 1) | Bark | 로컬 GPU, 범용 |
| TTS (폴백 2) | Edge TTS | 클라우드, 목소리 복제 불가 |
| 참조 음성 변환 | ffmpeg + ffprobe | WAV 변환, 3~8초 트리밍 |
| GUI | CustomTkinter | 다중 엔진 지원 UI |
| API 통신 | requests | GPT-SoVITS 서버 호출 |

## 게이트 / 검증
- [x] 6개 엔진 후보 비교 평가 완료
- [x] GPT-SoVITS 서버 실행 및 API 통신 정상
- [x] `gpt_sovits_engine.py` 음성 생성 테스트 통과
- [x] 3명 팀원 목소리 녹음 및 등록 완료
- [x] 앱에서 GPT-SoVITS 엔진 선택 후 음성 생성 정상
- [x] 참조 음성 자동 트리밍 (3~8초) 동작 확인
