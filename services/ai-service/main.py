"""
QT-AI Service — 강상민

FastAPI 진입점. ChromaDB RAG + Anthropic Claude API SSE 스트리밍.

사용:
    uvicorn main:app --reload --port 8085

주의:
    - 이 서비스는 Python FastAPI 전담입니다. Java/Spring Boot 코드를 추가하지 마세요.
    - LLM: Anthropic Claude API 사용 (다른 공급자 교체 금지 — DECISIONS.md §6)
    - Kafka envelope: `data` 키 사용 (`payload` 금지)
"""
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from routers import session


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: ChromaDB 연결, Kafka Producer 초기화 등
    print("[ai-service] Starting up...")
    yield
    # Shutdown: 연결 종료
    print("[ai-service] Shutting down...")


app = FastAPI(
    title="QT-AI Service",
    description="AI coaching service for Bible meditation",
    version="0.1.0",
    lifespan=lifespan,
)

# CORS — 프로덕션에서는 Gateway에서 차단
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routes
app.include_router(session.router, prefix="/ai", tags=["session"])


@app.get("/actuator/health")
async def health():
    """Spring Boot 스타일 health check (다른 서비스와 일관성)"""
    return {"status": "UP"}


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("AI_SERVICE_PORT", "8085"))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
