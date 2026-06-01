# 2026-05-21 Flutter 프로젝트 초기 설정

## 목표
Flutter 앱의 공통 인프라(네트워크, 에러 처리, 상태관리, 테마)를 구축한다. 이후 Phase에서 각 화면(인증, 마이페이지, 온보딩 등)을 올릴 때 기반이 준비되어 있도록 뼈대를 잡는다.

## 작업 내용
1. **디렉토리 구조 설계** — config/, core/(error/network/theme/widgets), features/, routes/ 구조 확립
2. **환경별 설정** — `--dart-define=ENV` 방식으로 dev/staging/prod baseUrl 분리
3. **네트워크 인터셉터** — ErrorInterceptor(DioException→AppException 변환), AuthInterceptor(JWT 자동 첨부)
4. **공통 위젯 4개** — LoadingWidget, AppErrorWidget, EmptyWidget, AsyncValueWidget(Riverpod AsyncValue 자동 분기)
5. **상태관리** — Riverpod 2.x 채택 (타입 안전, 테스트 용이)
6. **의존성 선정** — flutter_riverpod, dio, flutter_secure_storage, logger, intl 등. Retrofit은 코드 생성 의존성 과도하여 제거

## 범위
- 브랜치: `feature/flutter-init`
- PR Guard 리뷰 없이 1회 제출로 머지
- `flutter analyze` info 1건 (개발 로그 print — 의도적 유지)
- `flutter pub get` 의존성 109개 정상 설치

## 미해결
- 각 도메인 화면(auth, home, mypage)은 후속 Phase에서 구현

## 담당
- DevD 이승욱
