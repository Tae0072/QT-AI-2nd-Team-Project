# 2026-06-11 성경 본문 전체 페이지 + 해설 진입점 + 노트 굵게 버그 — 결과 보고

## 요약
성경 탭 3건 완료: ① 노트 상세에서 굵게 등 마크업이 평문으로 노출되던 버그 수정(F-03), ② 본문 조회 결과를 바텀시트 → 전체 페이지(F-01), ③ QT와 동일한 해설 진입점을 정식 서버 API 확장으로 연동(F-01/F-08). 모듈 순환 방지를 위해 조합 로직은 qt 도메인에 두었다.

## 산출물
| 파일 | 설명 |
|------|------|
| `flutter-app/.../note/screens/note_detail_screen.dart` | 상세 보기 마크업 렌더(`_RichNoteText`, 편집기와 동일 파서) |
| `flutter-app/.../bible/screens/bible_passage_screen.dart` (신규) | 본문 전체 페이지 + 해설 버튼 게이팅 |
| `flutter-app/.../bible/screens/bible_browser_screen.dart` | 조회 시 전체 페이지로 push, 바텀시트·죽은 코드 제거 |
| `flutter-app/.../bible/models/bible_models.dart` | `BiblePassageStudy` 모델 |
| `flutter-app/.../bible/services/bible_repository.dart` · `providers/bible_providers.dart` | `/qt/passage-study` 조회 + provider |
| `qtai-server/.../qt/api/GetBiblePassageStudyUseCase.java` · `dto/BiblePassageStudy.java` (신규) | qt 공개 계약 |
| `qtai-server/.../qt/internal/QtService.java` · `QtPassageRepository.java` · `BibleBookLookup.java` | 범위→QT본문→승인 해설 조합 |
| `qtai-server/.../qt/web/QtController.java` | `GET /api/v1/qt/passage-study` |
| `qtai-server/apis/api-v1/openapi.yaml` | 경로·스키마 |
| 테스트 | `bible_passage_screen_test`(3), `QtServiceTest`(3 신규), `bible_browser_screen_test`(갱신) |

## 검증
- Flutter: `flutter analyze`(bible/note) 무이슈, `flutter test` bible+note 39 + 신규 3 통과
- 서버: `./gradlew :service-bible:test` 전체 통과, OpenAPI 파싱·$ref OK

## 리뷰 보완 (2026-06-11)
- ① 부정 분기·통합 테스트: `QtServiceTest`에 권 미존재·해설 없음 케이스 추가(총 5 getPassageStudy 케이스), 사용자 `QtController` MockMvc 통합 테스트 `QtPassageStudyApiTest` 신규(해설 있음/매핑 없음/권 미존재/미인증 401).
- ② 저장소: `findContainingRange`에 `@Param` 부여 + `QtPassageRepositoryTest`(@DataJpaTest) 슬라이스 테스트(포함/범위초과/다른장) 신규.
- 검증: 3개 테스트 클래스 통과(QtServiceTest 12, QtPassageStudyApiTest 4, QtPassageRepositoryTest 3 — 0 fail/error).
- ③ §8 위반(개역·KJV seed) 에스컬레이션: `reports/2026-06-11_ESCALATION_bible-seed-forbidden-data.md` 작성(별건, 머지 전 Lead 확인 필요).

## 미해결 / 후속
- qt/study 백엔드는 타 담당 도메인 — 머지 전 도메인 오너(DevA·AI) 리뷰 권장
- ③ `generate_seed_sql.py`(개역·KJV) §8 위반은 #231로 dev에 이미 존재 — 별도 에스컬레이션 처리
- 실기기 수동 확인 스크린샷 보강

담당: DevD 이승욱
