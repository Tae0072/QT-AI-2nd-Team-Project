# AI Service — 강상민
#
# 역할: AI 코칭 서비스 (Python FastAPI 전담)
# 담당: ChromaDB RAG, Anthropic Claude API SSE 스트리밍, 큐티 A~D 프롬프트
# 포트(로컈): 8085
#
# 참조 명세: https://github.com/Tae0072/2nd-Team-Project/blob/main/apis/ai/openapi.yaml
#
# ⚠️ Spring Boot / Java 코드 절대 금지
#
# 권장 프로젝트 구조:
#   main.py
#   requirements.txt
#   routers/session.py
#   rag/chroma_client.py
#   rag/embedder.py
#   prompts/templates.py
#   kafka/event_publisher.py
