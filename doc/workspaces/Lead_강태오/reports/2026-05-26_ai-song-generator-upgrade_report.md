# 2026-05-26 AI 노래 생성기 고도화 — 결과 보고

## 요약
05-21 초기 구현(4단계 찬양가 생성기)을 전면 고도화하여 실사용 가능한 AI 노래 생성기로 발전시켰다.
MusicGen GPU 가속, 실시간 진행률, XTTS v2 목소리 복제, 원클릭 자동 파이프라인,
Bark CPU 강제 모드를 포함한 완전한 프로토타입을 완성했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `C:\SongGen\app.py` | 고도화된 Gradio 앱 (5탭: 원클릭/가사/음악/보컬/믹싱) |
| `C:\SongGen\setup.bat` | 의존성 설치 스크립트 (Bark, XTTS v2, MusicGen 포함) |
| `C:\SongGen\실행.bat` | 앱 구동 배치 파일 |

## 구현 상세

### Gradio 6.x 호환성
`theme`·`css`를 `Blocks` 생성자에서 `demo.launch()`로 이동했다.
`show_copy_button` 파라미터는 6.x에서 제거되어 삭제했다.
단계 간 자동 연결은 `.then(fn=lambda ..., inputs=..., outputs=...)` 체이닝으로 구현했다.

### MusicGen GPU 가속 (CUDA OOB 패치)
`model.config.decoder.vocab_size = 2049` 패치를 `.to("cuda")` 이전에 적용했다.
이는 모델의 임베딩 테이블 크기(2049)와 설정값(2048) 불일치로 인한
device-side assert를 해결한다. CUBLAS 오류는 CUDA 연산을 메인 스레드에만
실행하고 스피너 스레드에서는 `progress()` 호출만 허용하는 방식으로 방지했다.

### XTTS v2 목소리 복제
`coqui-tts 0.27.5`를 Python 3.12 호환 방식으로 설치했다.
`transformers.pytorch_utils`에 누락된 `isin_mps_friendly` 속성을
`torch.isin`으로 패치하여 ImportError를 해결했다.
`COQUI_TOS_AGREED=1` 환경변수로 인터랙티브 라이선스 동의 prompt를 우회했다.

### Bark CPU 강제 모드
Bark 모델 가중치가 CUDA 텐서로 캐시된 상태에서 로드하면
`torch.load`가 "CUDA device에 역직렬화 시도" 오류를 발생시킨다.
`is_available=False` 패치는 역직렬화 자체를 실패시키므로 역효과였다.
최종 해결책: `torch.serialization.default_restore_location`을 패치하여
모든 CUDA 텐서 복원 시도를 CPU로 강제 리다이렉트했다.

```python
def _bark_cpu_context():
    import torch.serialization as _tser
    _orig = _tser.default_restore_location
    def _force_cpu(storage, location):
        if isinstance(location, str) and location.startswith("cuda"):
            return _orig(storage, "cpu")
        return _orig(storage, location)
    _tser.default_restore_location = _force_cpu
    return _tser, _orig
```

### 원클릭 자동 파이프라인
`auto_pipeline()` 함수가 5단계(가사→분석→음악→보컬→믹싱)를 순서대로 실행한다.
Ollama가 생성된 가사를 분석해 MusicGen용 영어 프롬프트를 자동 생성하는
`_core_music_prompt_from_lyrics()` 가 2단계(20~28%)에서 실행된다.

## 발생한 문제와 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| CUDA device-side assert | vocab_size=2048 vs 임베딩 테이블 2049 불일치 | `.to("cuda")` 전에 `vocab_size=2049` 패치 |
| CUBLAS_STATUS_EXECUTION_FAILED | 데몬 스레드에서 CUDA 연산 실행 | CUDA 연산 메인 스레드 전용, 스레드는 progress()만 |
| XTTS ImportError (isin_mps_friendly) | transformers에서 해당 함수 제거됨 | `_tpu.isin_mps_friendly = torch.isin` 패치 |
| XTTS 라이선스 prompt로 앱 멈춤 | 인터랙티브 y/n 입력 대기 | `COQUI_TOS_AGREED=1` 환경변수 |
| Bark CUDA 역직렬화 오류 | 캐시된 가중치가 CUDA 텐서로 저장됨 | `default_restore_location` 패치로 CPU 강제 |
| callback/callback_interval model_kwargs 오류 | `model.generate()` 미지원 파라미터 전달 | 해당 파라미터 제거 |

## 검증
- [x] MusicGen GPU 가속 정상 (CUDA OOB 패치)
- [x] 실시간 프로그레스바 전 단계 동작
- [x] XTTS v2 로드 및 목소리 복제 동작
- [x] Bark CUBLAS/역직렬화 오류 없이 보컬 생성
- [x] 원클릭 파이프라인 5단계 완전 자동 실행
- [x] 단계 간 자동 연결 (가사→보컬 입력, 음악→믹싱 입력 자동 채움)

## 한계 및 향후 과제
- **보컬 품질**: Bark는 TTS 기반으로 음정/창법 없음. "낭독 + 배경음악" 수준
- **한국어 노래 생성 미지원**: 진짜 노래 목소리를 내려면 가창 합성 전용 모델 필요
- **향후**: YuE (MAP Research, 2025) 등 오픈소스 가창 합성 모델로 전환 검토