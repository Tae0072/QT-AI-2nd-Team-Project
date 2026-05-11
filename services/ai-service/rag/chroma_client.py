"""
ChromaDB 클라이언트

DECISIONS.md §6: ChromaDB 단독 Vector Store
"""
import os
import chromadb


def get_chroma_client():
    """ChromaDB HTTP 클라이언트 반환"""
    host = os.getenv("CHROMADB_HOST", "localhost")
    port = int(os.getenv("CHROMADB_PORT", "8000"))
    return chromadb.HttpClient(host=host, port=port)


def get_or_create_collection(name: str = "bible_passages"):
    """성경 구절 collection 조회 또는 생성"""
    client = get_chroma_client()
    return client.get_or_create_collection(name=name)
