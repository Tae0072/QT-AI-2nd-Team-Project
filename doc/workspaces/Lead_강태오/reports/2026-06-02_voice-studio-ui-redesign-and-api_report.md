# 2026-06-02 Voice Studio UI 리뉴얼 및 REST API/MCP 서버 구축 결과 보고

> **[비제품 범위]** Lead 개인 기술 실험·R&D 기록입니다.

## 요약
Voice Studio 앱의 프로토타입 UI를 상용화 수준으로 전면 리뉴얼했다.
사이드바 네비게이션, 다크/라이트 테마 전환, 설정 페이지, 목소리 카드 리스트(선택/삭제/페이징),
진행 상태바, 콘솔 토글, 스크롤 기능을 구현했다.
외부 연동을 위해 FastAPI REST API 서버와 FastMCP 서버를 새로 작성하고,
API 토큰 발급/인증 시스템을 앱 설정에 통합했다.
설정은 JSON 파일에 영속화되어 재시작 시에도 유지된다.
버전을 v1.0으로 정리했다.

## 산출물
| 파일 | 설명 |
|------|------|
| `bible-tts/tts_app.py` | UI 전면 리뉴얼 (~2100줄) |
| `bible-tts/tts_api.py` | REST API 서버 (FastAPI, 포트 8090) |
| `bible-tts/tts_mcp.py` | MCP 서버 (FastMCP, stdio) |
| `bible-tts/API_GUIDE.md` | API/MCP 사용 가이드 |
| `bible-tts/settings.json` | 설정 영속화 파일 (자동 생성) |
| `bible-tts/TTS앱실행.vbs` | 터미널 숨김 바로가기 스크립트 |

## 아키텍처

### 앱 구조 (리뉴얼 후)
```
TTSApp(ctk.CTk)
├── root_frame
│   ├── sidebar (64px)
│   │   ├── VS 로고
│   │   ├── 🔊 음성 생성 버튼
│   │   ├── 🎙 목소리 등록 버튼
│   │   └── ⚙ 설정 버튼
│   └── main_area
│       ├── 헤더 (제목 + v1.0)
│       └── page_container
│           ├── gen (CTkScrollableFrame)
│           │   ├── 텍스트 입력 카드
│           │   ├── 컨트롤 카드 (목소리/반영률)
│           │   ├── 버튼 행
│           │   ├── 진행 상태바 (동적)
│           │   └── 콘솔 (토글)
│           ├── rec (CTkScrollableFrame)
│           │   ├── 가이드 카드 (14pt)
│           │   ├── 통합 카드 (녹음+등록+목록)
│           │   └── 콘솔 (토글)
│           └── settings (CTkScrollableFrame)
│               ├── 디스플레이 카드
│               ├── API 토큰 카드
│               ├── 앱 정보 카드
│               └── 콘솔 (토글)
```

### 테마 시스템
```
THEME_DARK                    THEME_LIGHT
─────────────                 ─────────────
bg_dark:  #0f0f1a (딥다크)    bg_dark:  #f5f6f8 (쿨그레이)
sidebar:  #161625             sidebar:  #3b1f12 (다크브라운)
accent:   #00d4aa (민트)      accent:   #ff7a3d (코럴)
text:     #e8e8f0             text:     #2c1810

_toggle_theme() → UI 전체 재구축 (root_frame destroy → rebuild)
```

### REST API 구조
```
FastAPI (포트 8090)
├── GET  /              — 상태 확인 (토큰 불필요)
├── GET  /voices        — 목소리 목록 (Bearer 인증)
├── POST /generate      — 음성 생성 (Bearer 인증)
├── GET  /generate/{f}  — WAV 다운로드 (Bearer 인증)
├── POST /voices/register — 목소리 등록 (Bearer 인증)
└── DELETE /voices/{n}  — 목소리 삭제 (Bearer 인증)

인증: Authorization: Bearer vs_xxxxxxxxx...
토큰: 앱 설정에서 발급, settings.json에 저장
```

### MCP 서버 구조
```
FastMCP (stdio)
├── voice_studio_list_voices    — 목소리 목록
├── voice_studio_generate       — 음성 생성
├── voice_studio_register_voice — 목소리 등록
├── voice_studio_delete_voice   — 목소리 삭제
└── voice_studio_list_outputs   — 생성된 파일 목록
```

## 구현 상세

### UI 디자인 시스템
COLORS 딕셔너리로 전체 위젯 색상을 일괄 관리한다.
bg_dark, bg_sidebar, bg_card, bg_input, border, accent, text, text_sec, text_dim 등
18개 색상 키로 모든 위젯의 fg_color, text_color, border_color를 설정한다.
테마 전환 시 COLORS를 교체하고 UI를 재구축하는 방식이다.

### 진행 상태바
음성 생성 시 텍스트를 조각으로 나눈 뒤, 각 조각 생성마다 퍼센트를 갱신한다.
`_estimate_speak_seconds()`로 예상 총 시간을 계산하고,
실제 경과 시간을 기반으로 남은 시간을 재계산한다.
완료 시 소요 시간 표시 후 설정값(기본 5초) 후 자동 숨김된다.

### 설정 영속화
settings.json에 dark_mode, console_visible, autohide_seconds, api_token을
저장한다. 설정 변경 시 `_save_settings()`가 즉시 호출되어 파일에 기록되고,
앱 시작 시 `_load_settings()`로 복원된다.

### API 토큰 인증
`secrets.token_hex(24)`로 `vs_` 접두사 48자 토큰을 생성한다.
REST API는 `fastapi.security.HTTPBearer`로 모든 엔드포인트에 Bearer 인증을 적용하고,
요청의 토큰을 settings.json의 값과 비교한다.
MCP 서버는 로컬 stdio 전송이므로 별도 인증 없이 동작한다.

## 발생한 문제와 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| Edit 도구 파일 잘림 (1590줄에서 truncate) | bash VM 마운트와 Edit 도구 동기화 불일치 | PowerShell에서 직접 수정, bash 복원 후 이스케이프 수정 |
| 라이트 모드 누런 배경 | 초기 크림/베이지 팔레트가 피로감 유발 | 쿨 그레이(#f5f6f8) + 다크 브라운 사이드바(#3b1f12)로 변경 |
| 콘솔이 스크롤 범위 밖 | log_frame이 main_area에 직접 pack | 각 페이지 내부에 개별 콘솔 배치, 모든 콘솔에 동시 로그 |
| 테마 전환 시 위젯 색상 미갱신 | CTkFrame configure만으로 하위 위젯 색상 변경 불가 | 전체 UI destroy → rebuild 방식 채택 |

## 검증
- [x] tts_app.py — Python ast.parse 통과 (2100+ 줄)
- [x] tts_api.py — Python ast.parse 통과
- [x] tts_mcp.py — Python ast.parse 통과
- [x] 다크/라이트 테마 전환 동작 확인
- [x] 설정 영속화 — 재시작 시 테마/콘솔/토큰 유지
- [x] API 토큰 발급/복사/폐기 UI 동작
- [x] 스크롤 — 창 축소 시 콘솔까지 접근 가능
- [x] 바로가기 — 터미널 창 미노출

## 한계 및 향후 과제
- **API 서버 상시 운영**: 현재 로컬 PC에서만 동작. 서버 배포 시 GPU 인스턴스 필요
- **MCP mcp 패키지 미설치**: `pip install mcp` 필요 (앱 venv에 미포함)
- **테마 전환 깜빡임**: UI 재구축 시 잠깐 깜빡임 (1회성, 구조적 한계)
- **API Rate Limiting**: 현재 제한 없음. 운영 시 추가 필요
