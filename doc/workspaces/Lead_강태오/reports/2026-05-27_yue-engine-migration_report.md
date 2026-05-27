# 2026-05-27 AI 노래 생성기 — YuE 엔진 전면 교체 결과 보고

## 요약
MusicGen + Bark 기반 "낭독 + 배경음악" 파이프라인을 MAP Research의 YuE 가창 합성 모델로
전면 교체했다. 기존 4단계(가사/음악/보컬/믹싱)가 2단계(가사 → 노래)로 단순화되었으며,
보컬과 반주가 통합된 실제 노래 수준의 출력이 가능해졌다.
4070 Ti Super 16GB 머신 전용 zip 패키지를 완성했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `C:\SongGen\app.py` | YuE 기반 3탭 Gradio 앱 (341줄) |
| `C:\SongGen\setup_yue.bat` | YuE 전용 설치 스크립트 (git 체크, Windows 패치 포함) |
| `C:\SongGen\실행.bat` | UTF-8 인코딩 기반 앱 구동 배치 |
| `SongGen_YuE.zip` | 배포용 압축 파일 (3파일, 6.8KB) |

## 아키텍처 비교

### 이전 (MusicGen + Bark, 3060 Ti)
```
Ollama → 가사 생성
       ↓
MusicGen → 반주 WAV (GPU)
       ↓
Bark → TTS 읽기 WAV (CPU)
       ↓
수동 믹싱 → 최종 WAV
```
결과: "낭독 + 배경음악" (음정·창법 없음)

### 이후 (YuE, 4070 Ti Super)
```
Ollama → 가사 생성
       ↓
YuE Stage1(7B) → 음악 토큰 생성 (보컬+반주 통합)
       ↓
YuE Stage2(1B) → 음질 향상
       ↓
xcodec 디코더 + vocoder → 최종 MP3
```
결과: 실제 노래 (보컬 음정·반주 통합)

## 구현 상세

### Windows 호환 패치 (`setup_yue.bat`)
YuE의 `infer.py`는 Linux 전용 `flash_attention_2`를 기본으로 사용한다.
`setup_yue.bat`이 git clone 직후 Python 인라인 스크립트로 자동 패치한다:
- `attn_implementation="flash_attention_2"` → `"sdpa"` (PyTorch 내장)
- `torch.compile(model)` 비활성화 (Windows 안정성)

### 한국어 가사 → YuE 포맷 변환
```python
KO_SECTION = {
    "1절": "verse", "2절": "verse", "코러스": "chorus",
    "브릿지": "bridge", "도입부": "intro", ...
}
def lyrics_to_yue(lyrics):
    # [1절] → [verse], [코러스] → [chorus] 자동 변환
```

### YuE subprocess 실행 및 진행률
`subprocess.Popen`으로 `infer.py`를 `cwd=YUE_DIR`에서 실행한다.
stdout을 라인 단위로 읽어 키워드 기반으로 진행률을 업데이트한다:
- "Stage1 inference" → 20%
- "Stage 2" → 65%
- "Created mix" → 95%

출력 파일은 `output/yue_<timestamp>/*_mixed.mp3`로 저장된다.

### VRAM 관리 (16GB)
YuE는 Stage1(~14GB)과 Stage2(~2GB)를 순차 실행하며
Stage1 완료 후 `model.cpu()` + `torch.cuda.empty_cache()`로 VRAM을 해제한다.
이 덕분에 16GB에서 두 모델을 순차 실행할 수 있다.

## 발생한 문제와 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| `setup_yue.bat`에서 `>=` 리다이렉트 오류 | 배치 파일에서 `>` = 리다이렉트 연산자 | `pip install transformers accelerate` (버전 제약 제거) |
| `&&` 미지원 (배치 파일) | CMD에서 `&&` 미지원 | `cd YuE` / `git pull` / `cd ..` 분리 |
| `app.py` 파일 중복/손상 | bash sandbox와 Windows 파일시스템 동기화 지연 | Python 스크립트로 직접 라인 슬라이싱 후 재조합 |
| `_bark_cpu_context()` 3-tuple 언팩 오류 | 이전 코드 잔존: `_t, _oa, _ol = ...` | 2-tuple `_t, _oa`로 통일 |
| `infer.py` flash_attention_2 임포트 오류 | Windows에서 flash-attn 미지원 | `setup_yue.bat` 패치로 sdpa 대체 |

## 검증
- [x] `app.py` py_compile 문법 검증 통과
- [x] AST 파싱 + 반환값/outputs 개수 일치
- [x] `setup_yue.bat` 배치 문법 오류 없음 (errorlevel, git 체크)
- [x] 한국어 섹션 태그 변환 로직 정상
- [x] `SongGen_YuE.zip` 3파일 패키징 완료
- [ ] 4070 Ti Super 실기기 실행 테스트 (예정)

## 한계 및 향후 과제
- **한국어 발음**: YuE는 영어·중국어 위주 학습. 한국어 가사 발음 품질 제한적
- **생성 시간**: Stage1(7B LLM) 실행에 3~10분 소요 예상
- **첫 실행 모델 다운로드**: ~15GB HuggingFace 다운로드 필요
- **향후**: 한국어 특화 가창 합성 모델 연구 및 YuE 파인튜닝 검토