# 2026-05-21 Flutter 프로젝트 초기 설정 — 결과 보고

## 요약
Flutter 앱의 공통 인프라(네트워크/에러처리/상태관리/테마)를 구축했다. Riverpod 2.x, Dio 5.x, Navigator 1.0, flutter_secure_storage 채택. Retrofit은 코드 생성 의존성 과도하여 제거.

## 산출물

| 파일 | 설명 |
|------|------|
| `config/app_config.dart` | 환경별 설정 (dev/staging/prod baseUrl) |
| `core/error/app_exception.dart` | 공통 예외 클래스 |
| `core/network/api_client.dart` | Dio Provider |
| `core/network/api_response.dart` | 서버 공통 응답 래퍼 |
| `core/network/auth_interceptor.dart` | JWT 자동 첨부 |
| `core/network/error_interceptor.dart` | DioException → AppException 변환 |
| `core/theme/app_theme.dart` | Material 3 테마 |
| `core/widgets/` | LoadingWidget, AppErrorWidget, EmptyWidget, AsyncValueWidget |
| `routes/` | app_router.dart(Navigator 1.0), app_routes.dart |
| `main.dart` | 앱 엔트리포인트 |

## 검증
- `flutter analyze` — info 1건 (개발 로그 print, 의도적 유지)
- `flutter pub get` — 의존성 109개 정상 설치

## 미해결
- 각 도메인 화면(auth, home, mypage)은 후속 Phase에서 구현
