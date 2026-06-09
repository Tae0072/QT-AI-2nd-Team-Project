# -*- coding: utf-8 -*-
"""QT-AI 경량 TTS 서비스 (클라우드 배포용, Edge TTS 기반).

기존 Voice Studio(bible-tts/tts_api.py)는 GPU 커스텀 목소리까지 지원하지만
무겁고 로컬 PC에 묶여 있다. 이 서비스는 팀 전원이 쓸 수 있도록
GPU 없이(Edge TTS) CPU 클라우드에서 동작하는 최소 구현이다.

앱(flutter-app)의 TtsRepository가 기대하는 API 규격을 그대로 따른다:
- GET  /          헬스체크 (200)
- GET  /voices    목소리 목록 (Bearer)
- POST /qt/read   QT 본문 → 음성 (Bearer), [N초] 묵음 태그 지원

목소리 표시 이름은 기존 서버와 동일하게 유지해 저장된 설정(선희/인준/현수)이
그대로 작동한다.
"""
import io
import os
import re

import edge_tts
from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydub import AudioSegment
from pydantic import BaseModel

# 표시 이름 → Edge 음성 ID (기존 edge_tts_engine.py와 동일하게 유지)
KOREAN_VOICES = {
    "선희 (여성)": "ko-KR-SunHiNeural",
    "인준 (남성)": "ko-KR-InJoonNeural",
    "현수 (남성, 다국어)": "ko-KR-HyunsuMultilingualNeural",
}
DEFAULT_VOICE = "선희 (여성)"

# 문장 사이 기본 간격(ms)과 [N초] 태그
SENTENCE_GAP_MS = 400
SILENCE_TAG_RE = re.compile(r"\[(\d+(?:\.\d+)?)\s*초\]")
MAX_SILENCE_SEC = 30.0

API_TOKEN = os.environ.get("API_TOKEN", "")  # 비어 있으면 인증 생략(개발용)

app = FastAPI(title="QT-AI TTS Service", version="1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)
security = HTTPBearer(auto_error=False)


def verify_token(cred: HTTPAuthorizationCredentials = Depends(security)):
    """API_TOKEN이 설정돼 있으면 Bearer 토큰 일치를 요구한다."""
    if not API_TOKEN:
        return  # 토큰 미설정 환경(로컬 개발)에서는 인증 생략
    if cred is None or cred.credentials != API_TOKEN:
        raise HTTPException(status_code=401, detail="유효하지 않은 토큰입니다")


class QtReadRequest(BaseModel):
    text: str
    voice: str = DEFAULT_VOICE
    tau: float = 0.7  # 호환용(Edge TTS에서는 미사용)
    format: str = "mp3"


def _voice_id(display_name: str) -> str:
    return KOREAN_VOICES.get(display_name, KOREAN_VOICES[DEFAULT_VOICE])


def _split_silence_tags(text: str):
    """[N초] 태그 기준으로 ("text", 본문) / ("silence", 초) 세그먼트로 분해."""
    segments = []
    pos = 0
    for m in SILENCE_TAG_RE.finditer(text):
        before = text[pos:m.start()].strip()
        if before:
            segments.append(("text", before))
        try:
            sec = float(m.group(1))
        except ValueError:
            sec = 0.0
        if sec > 0:
            segments.append(("silence", min(sec, MAX_SILENCE_SEC)))
        pos = m.end()
    tail = text[pos:].strip()
    if tail:
        segments.append(("text", tail))
    return segments


async def _edge_mp3(text: str, voice_id: str) -> bytes:
    """Edge TTS로 한 조각을 mp3 바이트로 생성."""
    communicate = edge_tts.Communicate(text, voice_id)
    buf = bytearray()
    async for chunk in communicate.stream():
        if chunk["type"] == "audio":
            buf.extend(chunk["data"])
    return bytes(buf)


@app.get("/")
def root():
    return {"status": "ok", "service": "qt-ai-tts", "engine": "edge-tts"}


@app.get("/voices")
def list_voices(_=Depends(verify_token)):
    return [
        {
            "name": name,
            "display_name": name,
            "type": "edge",
            "has_recording": False,
            "has_finetuned": False,
        }
        for name in KOREAN_VOICES
    ]


@app.post("/qt/read")
async def qt_read(req: QtReadRequest, _=Depends(verify_token)):
    text = (req.text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="텍스트를 입력해 주세요")

    voice_id = _voice_id(req.voice.strip())
    segments = _split_silence_tags(text)
    if not any(kind == "text" for kind, _ in segments):
        raise HTTPException(status_code=400, detail="유효한 텍스트가 없습니다")

    combined = AudioSegment.empty()
    prev_kind = None
    for kind, value in segments:
        if kind == "text":
            mp3 = await _edge_mp3(value, voice_id)
            if not mp3:
                continue
            seg = AudioSegment.from_file(io.BytesIO(mp3), format="mp3")
            if prev_kind == "text":
                combined += AudioSegment.silent(duration=SENTENCE_GAP_MS)
            combined += seg
        else:  # [N초] 묵음 태그
            combined += AudioSegment.silent(duration=int(value * 1000))
        prev_kind = kind

    if len(combined) == 0:
        raise HTTPException(status_code=500, detail="음성 생성에 실패했습니다")

    out = io.BytesIO()
    if req.format == "wav":
        combined.export(out, format="wav")
        media_type = "audio/wav"
    else:
        combined.export(out, format="mp3", bitrate="128k")
        media_type = "audio/mpeg"
    return Response(content=out.getvalue(), media_type=media_type)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app:app", host="0.0.0.0", port=int(os.environ.get("PORT", "8090")))
