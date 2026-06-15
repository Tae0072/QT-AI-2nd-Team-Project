# 2026-06-15 admin-system-notice-publish-delivery report

## 변경 요약

AD-06 시스템 공지 발행 응답에 실제 사용자 전달 확인용 필드를 보강했다.

- `data.noticeId`: 발행된 공지 ID
- `data.notificationResult.targetMemberCount`: 발행 대상 활성 회원 수
- `data.notificationResult.queuedCount`: 사용자 앱 알림함/폴링으로 전달 대기 상태가 된 `notifications` row 수
- 기존 호환 필드 `id`, `requestedCount`, `createdCount`, `failedCount`는 유지

관리자 웹 발행 완료 토스트는 `대상 n명, 알림함 n건 생성, 실패 n건` 형식으로 표시한다.

## 수동 테스트 방법

1. 관리자 웹에서 `시스템 공지` 탭으로 이동한다.
2. `공지 등록`을 눌러 제목/본문을 입력하고 등록한다.
3. 생성된 초안 행에서 `발행`을 누른다.
4. 브라우저 개발자 도구 Network에서 `POST /api/v1/admin/notices/{noticeId}/publish` 응답을 확인한다.
   - `data.noticeId`가 발행한 공지 ID와 같아야 한다.
   - `targetMemberCount`가 활성 회원 수와 같아야 한다.
   - `queuedCount`가 사용자 알림함에 생성된 알림 수와 같아야 한다.
   - `failedCount`는 정상 케이스에서 `0`이어야 한다.
5. DB에서 발행 결과를 확인한다.

```sql
select id, status, published_at
from notices
where id = :noticeId;

select count(*) as notice_notification_count
from notifications
where notice_id = :noticeId
  and type = 'NOTICE';
```

6. 테스트 사용자로 모바일 앱에 로그인한 뒤 `마이페이지 > 알림`을 열어 공지 제목/본문이 보이는지 확인한다.
7. 앱이 켜져 있으면 최대 20초 뒤 폴링이 unread NOTICE 알림을 감지해 로컬 알림 배너를 띄운다.

## 판정 기준

- `notices.status = PUBLISHED`
- `notifications.notice_id = noticeId`이고 `type = NOTICE`인 row가 `queuedCount`만큼 존재
- 테스트 사용자 알림 목록에 같은 제목/본문이 노출
- `failedCount > 0`이면 일부 회원 알림 생성 실패로 보고 서버 로그와 감사 로그의 `NOTICE_PUBLISH` snapshot을 함께 확인
