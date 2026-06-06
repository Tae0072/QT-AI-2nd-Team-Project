# 2026-06-05 TTS [N초] 묵음 태그 지원 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
TTS 텍스트에 `[1초]`, `[2.5초]` 같은 묵음 태그를 넣으면 해당 길이의 무음이
삽입되도록 Voice Studio TTS API(`/qt/read`)를 확장했다.
Flutter 앱은 본문+해설을 함께 읽을 때 **본문 → [2초] 묵음 → 해설** 순서로 낭독한다.

## 구현 내용

### 1. TTS 서버 (bible-tts/tts_api.py — 저장소 외부)
- `[N초]` 정규식(`\[(\d+(?:\.\d+)?)\s*초\]`) 파싱 헬퍼 `_split_silence_tags` 추가
- 텍스트를 (text/silence) 세그먼트로 분해 → text는 기존 문장 분할·생성,
  silence는 합치기 단계에서 `np.zeros(sr × N)` 무음으로 삽입
- 묵음 최대 30초 제한, 태그만 있고 텍스트가 없으면 400 처리
- 문장 사이 기본 0.4초 간격은 텍스트 조각 사이에만 적용 (묵음 태그와 중복 방지)
- 패치는 스크립트로 재현 가능: `flutter-app/tool/patch_tts_silence.py`
  (원본 백업: `tts_api.py.bak_silence`)

### 2. Flutter (qt_tts_button.dart)
- `_composeText`: 본문과 해설이 모두 있으면 `[2초]` 태그로 연결
- 캐시 키 범위 표시 갱신: `be` → `bes2` (묵음 포함 버전을 구버전 캐시와 구분)

## 변경 파일
| 파일 | 변경 내용 |
|------|----------|
| `flutter-app/lib/features/tts/widgets/qt_tts_button.dart` | 본문↔해설 사이 [2초] 태그 + 캐시 키 갱신 |
| `flutter-app/tool/patch_tts_silence.py` | TTS 서버 패치 스크립트 (재현용) |
| (외부) `bible-tts/tts_api.py` | [N초] 태그 파싱 + 무음 삽입 |

## 검증
- [x] 패치 후 Python 문법 검증(ast.parse) + 서버 재기동 정상
- [x] API 직접 호출 비교 (선희, 동일 2문장):
  - 태그 없음: 4.73초 / `[3초]` 포함: 8.59초 (+3.86초)
  - 파형 분석: 중간에 4.0초 무음 구간 확인 (3초 태그 + 문장 끝 자연 무음)
- [x] flutter analyze: No issues found
- [x] flutter test: 100개 전체 통과

## 참고
- tts_api.py는 Lead 개인 환경(Downloads/bible-tts)이라 저장소에는 패치 스크립트만 보관
- TTS 서버 실행은 venv 필수: `venv\Scripts\python.exe tts_api.py` (시스템 python에는 numpy 없음)
