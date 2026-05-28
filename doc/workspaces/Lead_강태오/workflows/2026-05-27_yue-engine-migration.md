# 2026-05-27 AI 노래 생성기 — YuE 엔진 전면 교체

## 목표
MusicGen(반주) + Bark(TTS) 조합의 구조적 한계("낭독 + 배경음악" 수준)를 해결하고
실제 보컬과 반주가 통합된 진짜 노래를 생성한다.
MAP Research의 오픈소스 가창 합성 모델 YuE를 로컬에서 실행하도록
파이프라인 전체를 재설계한다.

## 범위
- **엔진 전환**: MusicGen + Bark + XTTS → YuE 단일 모델로 교체
- **파이프라인 단순화**: 4단계(가사/음악/보컬/믹싱) → 2단계(가사/노래)
- **YuE Windows 호환**: flash_attention_2 → sdpa 패치, torch.compile 비활성화
- **VRAM 최적화**: 4070 Ti Super 16GB에서 Stage1(7B) + Stage2(1B) 순차 실행
- **앱 재작성**: 새로운 3탭 UI (원클릭/가사생성/YuE 노래생성)
- **배포 패키지**: setup_yue.bat + app.py + 실행.bat → SongGen_YuE.zip

## 단계

1. **엔진 선정 및 VRAM 분석**
   - 3060 Ti(8GB) / 4070 Ti Super(16GB) 두 환경 비교
   - YuE 최소 요구: ~12GB (양자화 기준), 풀 모델: ~24GB
   - 결론: 4070 Ti Super(16GB)에서 YuE Stage1(7B, bfloat16≈14GB) + Stage2(1B) 순차 실행 가능
   - 3060 Ti는 YuE 실행 불가 → 4070 Ti Super 전용 패키지로 결정

2. **YuE 파이프라인 분석**
   - GitHub 레포(`multimodal-art-projection/YuE`) `inference/infer.py` 소스 분석
   - 입력: `genre.txt`(영어 스타일 태그) + `lyrics.txt`(`[verse]`/`[chorus]` 포맷)
   - 출력: `output/<name>_mixed.mp3` (보컬 stem + 반주 stem 자동 믹스)
   - Stage1(LLM) → Stage2(업샘플러) → xcodec 디코더 → vocoder 후처리 구조 파악

3. **Windows 호환 패치 설계**
   - `flash_attention_2`: Linux 전용 커스텀 CUDA 커널 → Windows 미지원
   - 대체: `attn_implementation="sdpa"` (PyTorch 내장 Scaled Dot-Product Attention)
   - `torch.compile`: Windows에서 간헐적 오류 → 비활성화
   - 패치는 `setup_yue.bat`의 Python 인라인 스크립트로 클론 후 자동 적용

4. **한국어 섹션 태그 변환 구현**
   - `[1절]`, `[코러스]`, `[브릿지]` 등 한국어 태그 → YuE 포맷 자동 변환
   - `lyrics_to_yue()` 함수: 정규식으로 섹션 헤더 추출 후 `KO_SECTION` 딕셔너리 매핑
   - `[verse]`, `[chorus]`, `[bridge]`, `[intro]`, `[outro]`, `[pre-chorus]`

5. **장르별 영어 프롬프트 매핑**
   - 10개 장르(찬양가/K-pop/발라드/가요/재즈/팝/R&B/힙합/컨트리/시네마틱) 정의
   - 각 장르별 YuE 최적 태그: 악기 + 분위기 + 보컬 성별/음색 조합
   - 예: `"Korean ballad, emotional, piano, strings, slow, heartfelt female vocal"`

6. **Gradio 앱 재작성**
   - 탭 구성: [🚀 원클릭 자동] | [📝 가사 생성] | [🎵 YuE 노래 생성]
   - `run_yue()`: subprocess로 `YuE/inference/infer.py` 호출, stdout 파싱으로 진행률 표시
   - `_ScaledProgress`: 원클릭 파이프라인 내 YuE 진행률을 20~100% 구간으로 스케일
   - 가사 탭 → YuE 탭 자동 연결 (`.then()` 체이닝)

7. **설치 스크립트 작성 및 패키징**
   - `setup_yue.bat`: git/Python 존재 확인 → venv312 생성 → PyTorch CUDA 12.1
     → 기본 패키지 → YuE 클론 → YuE 의존성 → Windows 패치 자동 적용
   - `실행.bat`: UTF-8(chcp 65001) + 절대 경로 기반 구동
   - `SongGen_YuE.zip` (app.py + setup_yue.bat + 실행.bat) 패키징

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| 로컬 LLM | `ollama` + `qwen2.5:7b` | 한국어 가사 생성 |
| 가창 합성 | YuE (m-a-p/YuE-s1-7B, YuE-s2-1B) | 보컬+반주 통합 생성 |
| 어텐션 | `sdpa` (PyTorch 내장) | flash_attention_2 Windows 대체 |
| UI | `gradio` 6.x | Blocks + Tabs |
| 실행 방식 | `subprocess.Popen` | YuE infer.py 비동기 실행 + 진행률 파싱 |

## 게이트 / 검증
- [x] `setup_yue.bat` 문법 및 로직 검증 (git 체크, errorlevel 분기)
- [x] `app.py` AST 파싱 + py_compile 문법 검증
- [x] 반환값·outputs 개수 일치 확인 (auto_pipeline 3, generate_lyrics 2, generate_song 2)
- [x] 단계 간 자동 연결 검증 (가사→YuE 탭 자동 채움)
- [x] YuE 미설치 시 에러 메시지 출력 (`YUE_OK` 체크)
- [x] `SongGen_YuE.zip` 패키징 완료 (3파일, 6.8KB)
- [ ] 실기기 테스트 (4070 Ti Super 머신에서 실행 예정)

> **[미구현 항목 안내]** 위 체크리스트에서 - [ ]로 표시된 항목은
> 현재 미구현 상태이며, 회의 확정 또는 일정에 따라 추후 구현될 예정입니다.
> 해당 항목이 구현되면 - [x]로 변경하고 별도 워크플로우·리포트를 작성합니다.