# 2026-05-27 Flutter 온보딩 화면 구현

## 목표
앱 최초 설치 시 표시되는 온보딩 화면(4페이지)을 구현한다.
스와이프 + Skip + dot indicator 네비게이션을 제공하며,
완료 후 카카오 로그인 화면으로 이동한다.

## 관련 F-ID
- F-16 마이페이지 (온보딩은 앱 첫 진입 UX로 마이페이지 범위에 포함)

## 기준 문서
- `06_화면_기능_정의서.md` §온보딩
- `07_요구사항_정의서.md` v3.1 §6.1 (앱 진입)

## 범위
- **모델**: `OnboardingPageData` — 페이지별 제목/설명/아이콘/색상 데이터
- **화면**: `OnboardingScreen` — PageView 기반 4페이지 온보딩
- **위젯**: `OnboardingPageWidget` (개별 페이지), `PageIndicator` (dot indicator)
- **Provider**: `OnboardingNotifier` + SharedPreferences 연동
- **라우팅**: `app_router.dart`에 `/onboarding` 경로 추가
- **테스트**: Widget 테스트 7건 + 모델 테스트 4건 + indicator 테스트 3건

## 단계

1. **OnboardingPageData 모델 생성**
   - 4페이지 기본 데이터: QT 묵상 / AI 해설 / 노트·공유 / 시작하기
   - 차분한 남색/보라 톤 그라데이션 배경

2. **OnboardingScreen 구현**
   - PageView + PageController
   - 상단 Skip(건너뛰기) 버튼, 마지막 페이지에서 숨김
   - 하단 dot indicator + 다음/시작하기 버튼

3. **OnboardingPageWidget + PageIndicator 위젯**
   - 플레이스홀더 아이콘 박스 (디자이너 교체용)
   - 애니메이션 dot indicator

4. **Provider + SharedPreferences**
   - `onboardingCompleteProvider` + `OnboardingNotifier`
   - 최초 1회 표시 후 플래그 저장

5. **라우터 연결**
   - `/onboarding` 경로 + onComplete 콜백 전달

6. **테스트 작성**
   - 15건: 스와이프, 버튼, Skip, indicator, 모델 데이터

## 담당
- Lead 강태오
