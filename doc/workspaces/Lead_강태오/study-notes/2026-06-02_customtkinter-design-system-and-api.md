# 2026-06-02 CustomTkinter 디자인 시스템 + REST API/MCP 학습 노트

> **[비제품 범위]** Lead 개인 기술 학습 기록입니다.

## 1. CustomTkinter 디자인 시스템 구축 방법

### 색상 시스템 (COLORS 딕셔너리)

CustomTkinter는 위젯마다 `fg_color`, `text_color`, `border_color` 등을 개별 지정해야 한다.
전체 앱의 색상을 일관되게 관리하려면 **색상 딕셔너리**를 하나 만들어서 모든 위젯에서 참조하는 게 좋다.

```python
COLORS = {
    "bg_dark":    "#0f0f1a",   # 메인 배경
    "bg_card":    "#1a1a2e",   # 카드 배경
    "accent":     "#00d4aa",   # 포인트 색상
    "text":       "#e8e8f0",   # 기본 텍스트
    "text_sec":   "#8888a8",   # 보조 텍스트
    "border":     "#2a2a44",   # 보더
    ...
}
```

위젯 생성 시 `C = self.COLORS`로 짧게 쓰면 코드가 깔끔해진다.

### 테마 전환 (다크/라이트)

CustomTkinter의 `set_appearance_mode("dark"/"light")`는 기본 위젯 색상만 바꾼다.
`fg_color`, `text_color` 등을 직접 지정한 위젯은 영향을 받지 않는다.

해결법: **UI 전체 재구축**

```python
def _toggle_theme(self):
    self._is_dark_mode = not self._is_dark_mode
    self.COLORS = dict(self.THEME_DARK if self._is_dark_mode else self.THEME_LIGHT)
    ctk.set_appearance_mode("dark" if self._is_dark_mode else "light")

    # 현재 상태 저장
    current_page = self._current_page
    # UI 제거 후 재구축
    self.root_frame.destroy()
    self._build_sidebar()
    self._build_main_area()
    # 상태 복원
    self._switch_page(current_page)
```

이 방식의 장점은 모든 위젯이 새 색상으로 확실히 갱신된다는 것이고,
단점은 전환 시 0.1초 정도 깜빡임이 있다는 것이다.

### 사이드바 네비게이션 패턴

```python
# 사이드바 (고정 폭)
sidebar = ctk.CTkFrame(root, width=64, corner_radius=0)
sidebar.pack(side="left", fill="y")
sidebar.pack_propagate(False)  # 자식이 크기를 바꾸지 못하게

# 네비게이션 버튼
nav_btns = {}
for key, icon in [("gen", "🔊"), ("rec", "🎙"), ("settings", "⚙")]:
    btn = ctk.CTkButton(sidebar, text=icon, width=44, height=44,
                         command=lambda k=key: switch_page(k))
    btn.pack(pady=4)
    nav_btns[key] = btn

# 페이지 전환
def switch_page(key):
    for p in pages.values():
        p.pack_forget()
    pages[key].pack(fill="both", expand=True)
    # 활성 버튼 하이라이트
    for k, btn in nav_btns.items():
        btn.configure(fg_color=C["bg_card"] if k == key else "transparent")
```

### CTkScrollableFrame

일반 `CTkFrame`을 `CTkScrollableFrame`으로 바꾸면 내부 내용이 프레임을 넘칠 때 자동 스크롤된다.

```python
page = ctk.CTkScrollableFrame(container, fg_color="transparent",
    scrollbar_button_color=C["border"],
    scrollbar_button_hover_color=C["text_dim"])
page.pack(fill="both", expand=True)
```

주의: `CTkScrollableFrame` 안에서 `fill="both", expand=True`를 쓰면
내용이 프레임 높이를 넘길 때만 스크롤바가 나타난다.

### 설정 영속화 패턴

```python
SETTINGS_FILE = Path(__file__).parent / "settings.json"

def _load_settings(self):
    defaults = {"dark_mode": True, "console_visible": True}
    if SETTINGS_FILE.exists():
        saved = json.load(open(SETTINGS_FILE))
        for k, v in defaults.items():
            if k not in saved:
                saved[k] = v
        return saved
    return defaults

def _save_settings(self):
    json.dump({"dark_mode": self._is_dark, ...}, open(SETTINGS_FILE, "w"))
```

설정 변경 시마다 `_save_settings()`를 호출하면 앱 종료 후에도 유지된다.

---

## 2. FastAPI REST API 서버

### 기본 구조

```python
from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

app = FastAPI(title="My API", version="1.0")
security = HTTPBearer()

def verify_token(cred: HTTPAuthorizationCredentials = Depends(security)):
    if cred.credentials != load_saved_token():
        raise HTTPException(401, "Invalid token")
    return cred.credentials

@app.get("/data")
def get_data(token=Depends(verify_token)):
    return {"result": "..."}
```

핵심 개념:
- `Depends(verify_token)`: 모든 요청에 자동으로 인증 체크
- `HTTPBearer()`: `Authorization: Bearer xxx` 헤더를 자동 파싱
- `HTTPException(401)`: 인증 실패 시 401 반환

### 파일 업로드

```python
@app.post("/upload")
async def upload(file: UploadFile = File(...)):
    content = await file.read()
    # 임시 파일에 저장 후 처리
```

### 파일 다운로드

```python
from fastapi.responses import FileResponse

@app.get("/download/{filename}")
def download(filename: str):
    return FileResponse(path, media_type="audio/wav")
```

### Swagger UI

FastAPI는 자동으로 `/docs`에 Swagger UI를 생성한다.
브라우저에서 `http://localhost:8090/docs`를 열면 모든 엔드포인트를 테스트할 수 있다.

---

## 3. MCP 서버 (FastMCP)

### MCP란?

Model Context Protocol — Claude 같은 AI가 외부 도구를 호출하는 표준 프로토콜이다.
MCP 서버를 만들면 Claude Code나 Cowork에서 "음성 생성해줘" 같은 명령으로
직접 TTS 기능을 호출할 수 있다.

### FastMCP 기본 패턴

```python
from mcp.server.fastmcp import FastMCP
from pydantic import BaseModel, Field

mcp = FastMCP("my_mcp")

class InputModel(BaseModel):
    text: str = Field(..., description="입력 텍스트")

@mcp.tool(name="my_tool", annotations={"readOnlyHint": False})
async def my_tool(params: InputModel) -> str:
    result = do_something(params.text)
    return json.dumps(result)

if __name__ == "__main__":
    mcp.run(transport="stdio")
```

### Claude Code 설정

```json
{
  "mcpServers": {
    "voice-studio": {
      "command": "python.exe",
      "args": ["tts_mcp.py"]
    }
  }
}
```

### REST API vs MCP 차이

| 항목 | REST API | MCP |
|------|----------|-----|
| 프로토콜 | HTTP | stdio (JSON-RPC) |
| 클라이언트 | 브라우저, Postman, 코드 | Claude Code, Cowork |
| 인증 | Bearer 토큰 | 로컬이므로 불필요 |
| 용도 | 범용 외부 연동 | AI 에이전트 전용 |

---

## 4. API 토큰 보안

### 토큰 생성

```python
import secrets
token = f"vs_{secrets.token_hex(24)}"
# 예: vs_a3f7b2c91d4e8f0612...
```

`secrets.token_hex(24)`는 48자의 암호학적으로 안전한 랜덤 문자열을 생성한다.
`vs_` 접두사는 토큰이 Voice Studio용임을 식별하기 위한 것이다.

### 토큰 저장

settings.json에 평문 저장한다. 로컬 전용 앱이므로 현재는 이 수준으로 충분하다.
서버 배포 시에는 환경 변수나 암호화 저장소를 사용해야 한다.

---

## 5. 배운 점 정리

- CustomTkinter에서 커스텀 색상을 쓰면 `set_appearance_mode`만으로는 테마가 안 바뀐다.
  → UI 재구축 방식이 가장 확실하다.
- `CTkScrollableFrame`을 쓰면 창 크기에 관계없이 모든 내용에 접근할 수 있다.
- FastAPI의 `Depends()` 패턴은 인증을 한 줄로 추가할 수 있어서 편리하다.
- MCP는 AI 에이전트 전용 프로토콜이라 REST API와 용도가 다르다. 둘 다 만들어두면 활용도가 높다.
- 설정 영속화는 JSON 파일 하나로 간단하게 해결 가능하다.
