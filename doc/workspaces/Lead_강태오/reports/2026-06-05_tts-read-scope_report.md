# 2026-06-05 TTS 읽기 범위 설정 (본문/주석) 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
TTS 낭독 범위를 설정으로 제어할 수 있게 했다.
마이페이지 > 설정에 "본문 읽기 (한글)"와 "주석 읽기" 토글을 추가했고,
둘 다 켜면 **한글 본문을 먼저 읽은 후 이어서 주석(해설)을 읽는다.**

## 구현 내용

### 1. 설정 (SharedPreferences 영구 저장)
- `ttsReadBibleProvider` (`tts_read_bible`, 기본 켜짐)
- `ttsReadExplanationProvider` (`tts_read_explanation`, 기본 꺼짐)
- 설정 화면에 SwitchListTile 2개 추가, 둘 다 끄는 것은 차단(SnackBar 안내)

### 2. 주석(해설) 데이터 연동
- `GET /api/v1/qt/{qtPassageId}/study-content` (study 도메인) 호출 추가
- `BibleRepository.getQtStudyContent()` + `QtStudyContent`/`QtExplanationItem` 모델
- 절 순서대로 explanation 텍스트를 합쳐 낭독 텍스트 구성 (`readableText`)

### 3. 낭독 텍스트 조합 (QtTtsButton)
- `_composeText()`: 설정에 따라 [본문] → [주석] 순서로 결합
- 캐시 키에 범위 접미사 추가: `{날짜}_{목소리해시}_{b|e|be}` — 범위별 캐시 분리
- 주석이 없거나 조회 실패 시 본문만으로 자동 폴백 + SnackBar 안내
  (주석 전용 설정인데 주석이 없으면 안내만)
- 목소리/범위 설정 변경 시 `ref.listen`으로 자동 재준비

## 변경 파일
| 파일 | 변경 내용 |
|------|----------|
| `flutter-app/lib/features/tts/providers/tts_providers.dart` | 읽기 범위 Provider 2종 |
| `flutter-app/lib/features/mypage/screens/settings_screen.dart` | 본문/주석 토글 UI + 둘 다 끔 방지 |
| `flutter-app/lib/features/bible/models/bible_models.dart` | QtStudyContent 모델 |
| `flutter-app/lib/features/bible/services/bible_repository.dart` | study-content API 호출 |
| `flutter-app/lib/features/tts/widgets/qt_tts_button.dart` | 텍스트 조합 + 범위별 캐시 |
| `flutter-app/lib/features/bible/screens/today_qt_screen.dart` | qtPassageId 전달 |

## 검증 (에뮬레이터 E2E)
- [x] flutter analyze: No issues found
- [x] 설정 화면에 본문/주석 토글 표시·저장 확인
- [x] 본문만(기본): `{날짜}_{voice}_b.mp3` 생성·재생
- [x] 둘 다 켬: `{날짜}_{voice}_be.mp3` 생성 (7.4MB, 본문 단독 6.2MB 대비 주석 분량 추가 확인) → 즉시 재생
- [x] 주석 데이터 없을 때: 본문만으로 폴백 (b 캐시 사용)

## 테스트 데이터 시딩 (dev DB)
dev DB에 승인 해설이 없어 E2E를 위해 직접 시딩했다 (자작 텍스트, `source_label='DEV_TEST'`):
- `verse_explanations`: 1CO 3:1, 3:2 절에 APPROVED 해설 2건
- `qt_passage_verses`: qt_passage_id=4 ↔ 1CO 3:1-15 절 매핑 15건 (비어 있었음 — 해설 조회의 전제 조건)

정리 SQL (필요 시):
```sql
DELETE FROM verse_explanations WHERE source_label='DEV_TEST';
DELETE FROM qt_passage_verses WHERE qt_passage_id=4;
```

## 참고
- 본문+주석 결합 음성은 생성 시간이 더 길다(약 2~3분). 첫 진입 백그라운드 준비로 체감 완화
- Riverpod `invalidate` 시 `valueOrNull`이 이전 데이터를 유지해 버튼 위젯이 재생성되지 않음
  → 새로고침으로는 TTS 재준비가 트리거되지 않는다 (설정 변경 또는 앱 재시작 시 재준비)
