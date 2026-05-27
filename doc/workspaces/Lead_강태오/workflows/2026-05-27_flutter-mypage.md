# 2026-05-27 Flutter 마이페이지 화면 구현

## 목표
Flutter 앱의 마이페이지(F-16) 화면을 구현한다.
대시보드, 프로필 편집, 닉네임 변경, 회원 탈퇴 흐름을 포함한다.

## 관련 F-ID
- F-16 마이페이지

## 기준 문서
- `07_요구사항_정의서.md` v3.1 §6.7 (마이페이지)
- `06_화면_기능_정의서.md` §마이페이지
- `04_API_명세서.md` §/api/v1/me/**

## 범위
- **API 모델**: `DashboardResponse`, `MemberResponse` DTO 클래스
- **Repository**: `MyPageRepository` — `/api/v1/me` 관련 API 호출
- **Provider**: Riverpod `mypage_providers.dart` — 대시보드/프로필 상태 관리
- **화면**: `MyPageScreen` (대시보드), `ProfileEditScreen` (프로필 편집)
- **위젯**: `ProfileCard`, `QuickMenuCard`, `StatsCard`, `WithdrawDialog`
- **라우팅**: `app_router.dart`에 마이페이지/프로필 편집 경로 추가
- **테스트**: Widget 테스트 4건 + Repository 테스트 1건

## 단계

1. **API 모델 클래스 생성**
   - `DashboardResponse`, `MemberResponse` record 클래스
   - JSON 직렬화/역직렬화

2. **MyPageRepository 구현**
   - `getMyInfo()`, `getDashboard()`, `updateProfile()`, `changeNickname()`, `withdraw()`

3. **Riverpod Provider 구현**
   - `dashboardProvider`, `myInfoProvider` 등 상태 관리

4. **대시보드 화면 구현**
   - `MyPageScreen`: 프로필 카드 + 빠른 메뉴 + 통계 카드
   - `ProfileCard`: 닉네임, 프로필 이미지, 가입일
   - `QuickMenuCard`: 노트, 공유, 찬양 바로가기
   - `StatsCard`: QT 연속일수, 노트 수 등

5. **프로필 편집 + 닉네임 변경**
   - `ProfileEditScreen`: 닉네임 변경 (7일 잠금 정책 반영)
   - 닉네임 사용 가능 여부 실시간 확인

6. **탈퇴 다이얼로그**
   - `WithdrawDialog`: 확인 후 탈퇴 API 호출

7. **라우터 연결**
   - GoRouter에 `/mypage`, `/mypage/edit` 경로 추가

8. **테스트 작성**
   - `dashboard_response_test.dart`: JSON 파싱 검증
   - `mypage_repository_test.dart`: API 호출 Mock 검증
   - `profile_card_test.dart`: 위젯 렌더링 검증
   - `quick_menu_card_test.dart`: 메뉴 탭 동작 검증

## 담당
- Lead 강태오
