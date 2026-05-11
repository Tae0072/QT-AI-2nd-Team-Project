"""
AI Session Router

엔드포인트 (DECISIONS.md §3 기준):
    POST   /ai/sessions                  — 세션 시작
    POST   /ai/sessions/{id}/turns       — 대화 (SSE 스트리밍)  ← /messages 금지!
    GET    /ai/sessions/{id}             — 세션 조회
    GET    /ai/sessions                  — 세션 목록

SSE 이벤트 계약:
    turn_started → token → rag_sources → turn_completed → [DONE]
"""
from fastapi import APIRouter, HTTPException
from sse_starlette.sse import EventSourceResponse
from pydantic import BaseModel

router = APIRouter()


class StartSessionRequest(BaseModel):
    bookCode: str
    chapter: int
    verse: int
    promptType: str  # A, B, C, D


class TurnRequest(BaseModel):
    userMessage: str


@router.post("/sessions")
async def start_session(req: StartSessionRequest):
    """AI 세션 시작"""
    # TODO: 구현 — ChromaDB RAG + 세션 생성 + DB 저장
    raise HTTPException(status_code=501, detail="Not implemented yet")


@router.post("/sessions/{session_id}/turns")
async def create_turn(session_id: str, req: TurnRequest):
    """
    AI 대화 — SSE 스트리밍

    이벤트 순서: turn_started → token (반복) → rag_sources → turn_completed → [DONE]
    """
    async def event_generator():
        # TODO: 구현 — Claude API 스트리밍 + ChromaDB 검색
        yield {"event": "turn_started", "data": '{"sessionId":"' + session_id + '"}'}
        # ... token 이벤트 반복 ...
        # ... rag_sources 이벤트 ...
        yield {"event": "turn_completed", "data": '{}'}
        yield {"event": "[DONE]", "data": ""}

    return EventSourceResponse(event_generator())


@router.get("/sessions/{session_id}")
async def get_session(session_id: str):
    """세션 조회"""
    raise HTTPException(status_code=501, detail="Not implemented yet")


@router.get("/sessions")
async def list_sessions():
    """세션 목록"""
    raise HTTPException(status_code=501, detail="Not implemented yet")
