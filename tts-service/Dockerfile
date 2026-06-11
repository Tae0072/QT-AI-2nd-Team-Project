# QT-AI 경량 TTS 서비스 — CPU 전용 (Edge TTS)
FROM python:3.12-slim

# pydub의 mp3/wav 처리를 위해 ffmpeg 필요
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

# 클라우드 플랫폼이 PORT 환경변수를 주입하면 그 포트로 listen
ENV PORT=8090
EXPOSE 8090

# 헬스체크 (대부분의 플랫폼이 자체 헬스체크를 쓰지만 로컬 docker용)
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request,os; urllib.request.urlopen('http://127.0.0.1:'+os.environ.get('PORT','8090')+'/')" || exit 1

CMD ["sh", "-c", "uvicorn app:app --host 0.0.0.0 --port ${PORT:-8090}"]
