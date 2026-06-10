# 2026-06-09 · admin-web 권한 가드 스터디 노트

## 왜 토큰만으로 부족한가

관리자 API는 두 단계를 확인한다.

1. `members.role=ADMIN`
2. `admin_users.admin_role`

JWT에는 현재 `role=ADMIN`만 있고 세부 관리자 권한은 들어 있지 않다. 그래서 프론트가 JWT를 디코딩해서 `OPERATOR`, `REVIEWER`를 판단할 수 없다.

## 사용할 백엔드 계약

`GET /api/v1/admin/me`는 현재 로그인한 관리자 정보를 반환한다.

```json
{
  "adminUserId": 1,
  "memberId": 1,
  "adminRole": "OPERATOR"
}
```

프론트는 이 값을 기준으로 메뉴와 라우트를 제한한다. 보안의 최종 방어선은 백엔드이고, 프론트 가드는 사용자가 접근 가능한 화면만 보여주는 UX 방어선이다.

## 권한 규칙

- `SUPER_ADMIN`: 모든 화면 접근.
- `requiredRoles=[]`: 활성 관리자라면 접근 가능.
- `requiredRoles=[...]`: 해당 역할 또는 `SUPER_ADMIN`만 접근.
- 권한이 없으면 메뉴에서 숨기고, 직접 URL 접근은 권한 부족 화면으로 막는다.
