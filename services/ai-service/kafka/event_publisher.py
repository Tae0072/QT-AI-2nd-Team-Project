"""
Kafka Event Publisher

AI Service가 발행하는 이벤트:
    - ai.session.completed

Envelope 표준 (DECISIONS.md §4):
    {
      "eventId":         "evt_<ULID>",
      "eventType":       "ai.session.completed",
      "eventVersion":    1,
      "schemaSubject":   "ai.session.completed-value",
      "occurredAt":      "<ISO-8601>",
      "traceId":         "<trace>",
      "producerService": "ai-service",
      "idempotencyKey":  "ai.session.completed:<sessionId>",
      "data":            { ... }
    }

⚠️ `payload` 키 사용 금지 — 반드시 `data`
"""
import os
import json
from datetime import datetime, timezone

# from kafka import KafkaProducer  # TODO: 실제 구현 시 주석 해제


def build_envelope(
    event_type: str,
    data: dict,
    idempotency_key: str,
    trace_id: str = "",
    event_version: int = 1,
) -> dict:
    """Kafka envelope 생성 — DECISIONS.md §4 귀점"""
    return {
        "eventId": f"evt_{datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S%f')}",
        "eventType": event_type,
        "eventVersion": event_version,
        "schemaSubject": f"{event_type}-value",
        "occurredAt": datetime.now(timezone.utc).isoformat(),
        "traceId": trace_id,
        "producerService": "ai-service",
        "idempotencyKey": idempotency_key,
        "data": data,  # 절대 `payload` 아님
    }


def publish_session_completed(session_id: str, data: dict, trace_id: str = ""):
    """ai.session.completed 이벤트 발행"""
    envelope = build_envelope(
        event_type="ai.session.completed",
        data=data,
        idempotency_key=f"ai.session.completed:{session_id}",
        trace_id=trace_id,
    )
    # TODO: 실제 KafkaProducer 구현
    print(f"[Kafka] Would publish: {json.dumps(envelope, ensure_ascii=False)}")
