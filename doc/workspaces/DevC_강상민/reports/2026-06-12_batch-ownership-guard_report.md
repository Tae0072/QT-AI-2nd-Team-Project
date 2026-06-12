# Report - 2026-06-12 배치 소유권 중복 실행 점검

## 요약

로컬 Docker compose 환경에서 `admin-server`와 도메인 서비스가 같은 배치/워커를 동시에 실행할 수 있는지 점검했다.

확인 결과 AI worker, AI daily seed, SU 오늘 QT 수집, music seed는 이미 꺼져 있거나 기본값으로 방어되어 있었다. 다만 `JournalEventReprocessor`는 `service-note`와 `admin-server` 양쪽에 존재하고 기본값이 `true`인데, compose에서 admin-server 쪽을 끄는 설정이 없었다.

`admin-server`의 note 아웃박스 재처리기를 명시적으로 끄도록 `JOURNAL_REPROCESSOR_ENABLED=false`를 추가했다. 함께 AI daily seed도 명시적으로 `false`를 넣어 설정 의도를 더 분명하게 했다.

## 확인한 배치/워커

| 영역 | 소유 서비스 | admin-server 상태 | 조치 |
| --- | --- | --- | --- |
| AI generation worker | `service-ai` | `AI_GENERATION_WORKER_ENABLED=false` | 유지 |
| AI daily QT verse seed | `service-ai` | 기본 false, 명시값 없음 | `AI_DAILY_QT_VERSE_SEED_ENABLED=false` 추가 |
| SU 오늘 QT 수집 | `service-bible` | `QT_TODAY_SUM_ENABLED=false` | 유지 |
| Music seed | `service-bible` | `QTAI_MUSIC_SEED_ENABLED=false` | 유지 |
| Journal event reprocessor | `service-note` | 기본 true 위험 | `JOURNAL_REPROCESSOR_ENABLED=false` 추가 |
| Retention purge | `admin-server` | `QTAI_RETENTION_PURGE_ENABLED` 기본 정책 | 변경 없음 |
| Mission progress batch | `admin-server` | admin-server 소유 | 변경 없음 |

## 변경 내용

- `docker-compose.yml`
  - `service-admin.environment`에 `AI_DAILY_QT_VERSE_SEED_ENABLED=false` 추가
  - `service-admin.environment`에 `JOURNAL_REPROCESSOR_ENABLED=false` 추가
  - note 아웃박스 재처리기는 `service-note`가 소유한다는 주석 추가

## 검증 결과

```powershell
docker compose config
```

결과: `service-admin` 환경변수에 아래 값이 반영됨.

- `AI_DAILY_QT_VERSE_SEED_ENABLED=false`
- `AI_GENERATION_WORKER_ENABLED=false`
- `JOURNAL_REPROCESSOR_ENABLED=false`
- `QT_TODAY_SUM_ENABLED=false`
- `QTAI_MUSIC_SEED_ENABLED=false`

```powershell
docker compose up -d service-admin
```

결과: 성공

```powershell
docker exec qtai-admin-server printenv
```

결과: 위 비활성 플래그가 실제 컨테이너 환경변수로 주입됨.

컨테이너 상태:

- `qtai-admin-server`: `healthy`

## 후속 검토

- 운영/staging compose 또는 배포 매니페스트도 같은 소유권 기준인지 확인해야 한다.
- admin-server 복사본 구조가 유지되는 동안 새 배치를 추가할 때 owner service와 disabled env를 같이 문서화해야 한다.
- 아주 중요한 배치는 설정 실수에 대비해 DB lock/idempotency까지 검토하는 것이 안전하다.
