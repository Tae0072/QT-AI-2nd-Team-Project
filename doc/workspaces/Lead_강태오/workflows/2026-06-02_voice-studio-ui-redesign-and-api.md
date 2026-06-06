# 2026-06-02 Voice Studio UI 리뉴얼 및 REST API/MCP 서버 구축

> **[비제품 범위]** 이 문서는 qtai-server 제품에 반영되지 않는 **Lead 개인 기술 실험·R&D 기록**입니다.

## 목표
TTS 앱(Voice Studio)의 프로토타입 UI를 상용화 수준 디자인으로 전면 리뉴얼하고,
외부 앱/Claude에서 사용 가능한 REST API 서버와 MCP 서버를 구축한다.
설정 영속화, 다크/라이트 테마 전환, API 토큰 발급 등 부가 기능을 추가한다.

## 범위
- **UI 디자인 리뉴얼**: 탭 뷰 → 사이드바 네비게이션 전환, 다크 사이드바(B번) 디자인
- **다크/라이트 테마**: 다크(민트) + 라이트(코럴 오렌지+다크 브라운 사이드바) 전환
- **설정 페이지**: 다크 모드 스위치, 콘솔 토글, 상태바 자동 숨김 시간, API 토큰 발급
- **목소리 목록 개선**: 카드형 리스트, 선택/삭제/페이징 기능
- **진행 상태바**: 음성 생성 시 프로그레스 바 + 예상 시간 표시
- **콘솔 토글**: 설정에서 콘솔 온/오프, 모든 페이지에 공통 적용
- **스크롤 기능**: 모든 페이지(음성 생성, 목소리 등록, 설정) 스크롤 가능
- **설정 영속화**: settings.json 저장/로드 (테마, 콘솔, 자동숨김, API 토큰)
- **REST API 서버**: FastAPI 기반 TTS API (Bearer 토큰 인증)
- **MCP 서버**: FastMCP 기반 Claude 연동 도구 서버
- **바로가기 개선**: 터미널 창 완전 숨김 실행
- **버전 변경**: v3.0 → v1.0

## 단계

1. **UI 디자인 리뉴얼 (다크 사이드바)**
   - `CTkTabview` 제거 → 사이드바(64px) + 메인 영역 레이아웃
   - VS 로고 아이콘, 3개 네비게이션(음성 생성/목소리 등록/설정)
   - 카드 컴포넌트 기반 UI (둥근 모서리 + 보더)
   - 색상 시스템: COLORS 딕셔너리로 전체 위젯 색상 일괄 관리
   - 창 크기 750x750 → 900x700

2. **다크/라이트 테마 전환**
   - `THEME_DARK`: 딥 다크(#0f0f1a) + 민트(#00d4aa)
   - `THEME_LIGHT`: 쿨 그레이(#f5f6f8) + 다크 브라운 사이드바(#3b1f12) + 코럴(#ff7a3d)
   - `_toggle_theme()`: 전체 UI 재구축 방식 (root_frame destroy → 재생성)
   - `_build_sidebar()`, `_build_main_area()` 메서드 분리로 재구축 지원
   - CustomTkinter `set_appearance_mode("dark"/"light")` 연동

3. **설정 페이지 구현**
   - 디스플레이 카드: 다크 모드 스위치, 콘솔 스위치, 상태바 자동 숨김 (3/5/10/0초)
   - API 토큰 카드: 토큰 발급/복사/폐기 버튼, REST/MCP 주소 표시
   - 앱 정보 카드: 이름, 버전, 엔진, 프레임워크

4. **목소리 목록 UI 개선**
   - 기존 `CTkTextbox` → 카드형 리스트 (클릭 선택, 민트 테두리 하이라이트)
   - 페이징: 4개/페이지, ◀/▶ 버튼, 페이지 표시, 총 개수
   - 선택 버튼: 선택된 목소리를 음성 생성 페이지로 전환 + 자동 세팅
   - 삭제 버튼: 확인 다이얼로그 → 관련 파일(ref/se/recording/npz) 모두 삭제

5. **진행 상태바 구현**
   - 프로그레스 바 + 퍼센트 + 예상 남은 시간 + 조각 진행 표시
   - 1% 단위 진행, 실제 경과 시간 기반 남은 시간 재계산
   - Edge TTS / OpenVoice / GPT-SoVITS / bark 모든 엔진 연동
   - 완료 시 "생성 완료" + 소요 시간, 설정값 후 자동 숨김
   - 실패 시 빨간색 "생성 실패" 표시

6. **콘솔 토글 개선**
   - 각 페이지 하단에 개별 콘솔 프레임 배치 (스크롤 영역 안)
   - 로그 기록 시 모든 페이지 콘솔에 동시 삽입
   - 설정 스위치로 모든 페이지 콘솔 일괄 숨김/표시

7. **스크롤 기능**
   - 모든 페이지를 `CTkScrollableFrame`으로 변경
   - 콘솔도 스크롤 영역 안에 포함되어 창 축소 시에도 접근 가능

8. **설정 영속화**
   - `settings.json`: dark_mode, console_visible, autohide_seconds, api_token
   - 앱 시작 시 `_load_settings()` → 마지막 설정 그대로 적용
   - 설정 변경 시마다 `_save_settings()` 즉시 저장

9. **REST API 서버 (`tts_api.py`)**
   - FastAPI 기반, 포트 8090
   - Bearer 토큰 인증 (settings.json의 api_token과 비교)
   - 엔드포인트: GET /, GET /voices, POST /generate, GET /generate/{file},
     POST /voices/register, DELETE /voices/{name}
   - Swagger UI: http://localhost:8090/docs

10. **MCP 서버 (`tts_mcp.py`)**
    - FastMCP 기반, stdio 전송
    - 도구 5개: list_voices, generate, register_voice, delete_voice, list_outputs
    - Claude Code / Cowork에서 직접 호출 가능

11. **API 토큰 시스템**
    - `secrets.token_hex(24)`로 `vs_` 접두사 48자 토큰 생성
    - 앱 설정에서 발급/복사/폐기
    - REST API의 모든 엔드포인트에 Bearer 인증 적용
    - MCP는 로컬 stdio라 토큰 불필요

12. **바로가기 개선**
    - `TTS앱실행.vbs`: GPT-SoVITS 서버를 WindowStyle=0(완전 숨김)으로 실행
    - 서버 로그 → server_log.txt 리다이렉트
    - 앱: pythonw.exe로 터미널 없이 실행

## 기술 스택
| 역할 | 라이브러리 | 비고 |
|------|-----------|------|
| GUI | CustomTkinter | 다크/라이트 테마, 사이드바 네비게이션 |
| REST API | FastAPI + Uvicorn | 포트 8090, Bearer 인증 |
| MCP | FastMCP (mcp Python SDK) | Claude Code 연동 |
| 인증 | secrets + JSON | 랜덤 토큰 발급, settings.json 저장 |
| 음성 | Edge TTS + OpenVoice + GPT-SoVITS | 기존 엔진 유지 |

## 게이트 / 검증
- [x] 다크 모드 UI 렌더링 정상 (사이드바 + 카드 + 민트 포인트)
- [x] 라이트 모드 전환 정상 (다크 브라운 사이드바 + 코럴 포인트)
- [x] 테마 전환 시 페이지/콘솔 상태 복원 정상
- [x] 목소리 선택/삭제/페이징 동작 확인
- [x] 진행 상태바 퍼센트 + 예상 시간 표시 확인
- [x] 콘솔 토글 모든 페이지 동작 확인
- [x] 스크롤 기능 — 콘솔 포함 전체 스크롤 확인
- [x] settings.json 영속화 — 재시작 시 설정 유지
- [x] API 토큰 발급/복사/폐기 UI 동작
- [x] tts_api.py 구문 검증 통과
- [x] tts_mcp.py 구문 검증 통과
- [x] tts_app.py 구문 검증 통과 (ast.parse)
- [x] 바로가기 실행 시 터미널 창 미노출 확인
