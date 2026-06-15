# 2026-06-15 관리자 배경음악 관리 기능 구현 워크플로우

## 1. 작업 목적

현재 배경음악 관리 기능은 관리자 웹에 존재하지 않는다.
사용자 앱 기능을 새로 만들지 않고, **관리자 웹에서 배경음악 데이터를 등록·수정·숨김/노출 관리할 수 있는 기능만 구현**한다.

## 2. 현재 상태

- 관리자 웹에 배경음악 관리 페이지 없음
- 관리자 배경음악 API 없음 또는 미연결 상태
- 사용자 앱에는 신규 화면/기능 추가하지 않음
- 기존 앱 배경음악 사용 흐름이 있다면, 관리자에서 관리한 ACTIVE 데이터만 기존 흐름에 반영되도록 백엔드 조회 계약만 맞춘다

## 3. 구현 범위

### 포함

#### 백엔드

관리자용 배경음악 관리 API 추가.

예상 API:
- `GET /api/v1/admin/music-tracks`
- `POST /api/v1/admin/music-tracks`
- `PATCH /api/v1/admin/music-tracks/{id}`
- `POST /api/v1/admin/music-tracks/{id}/publish`
- `POST /api/v1/admin/music-tracks/{id}/hide`

기능:
- 배경음악 목록 조회
- 배경음악 등록
- 배경음악 수정/교체
- 노출 처리
- 숨김 처리
- 권한 검사
- 필요 시 감사 로그 기록

#### 관리자 웹

관리자 웹에 배경음악 관리 페이지 추가.

기능:
- 목록 테이블
- 등록 버튼
- 등록/수정 모달
- 발행/숨김 버튼
- 상태 태그
- 성공/실패 토스트

### 제외

- 사용자 앱 신규 화면 추가
- 사용자 앱 UI 변경
- 앱 재생 UX 변경
- 사용자별 배경음악 추천
- FCM/알림 연동
- 외부 음원 플랫폼 연동

## 4. 데이터 모델 검토

기존 음악 관련 테이블이 있으면 우선 재사용한다.

확인 대상:
- `music_tracks`
- `praise_songs`
- 기타 배경음악 관련 엔티티/테이블

예상 필드:
- `id`
- `title`
- `artist`
- `sourceType`
- `sourceUrl`
- `licenseNote`
- `status`
- `publishedAt`
- `hiddenAt`
- `createdAt`
- `updatedAt`

상태값 예시:
- `DRAFT`
- `ACTIVE`
- `HIDDEN`

## 5. 권한 정책

관리자 배경음악 API는 관리자 권한을 요구한다.

필수 조건:
- `members.role = ADMIN`
- `admin_users.admin_role = OPERATOR` 또는 `SUPER_ADMIN`

## 6. 구현 순서

### 1단계. 기존 구조 확인

1. `service-bible` 또는 음악/찬양 도메인에서 기존 음악 엔티티 확인
2. `music_tracks` 테이블 존재 여부 확인
3. 기존 사용자 앱 배경음악 조회 API가 있는지 확인
4. 관리자 웹 메뉴/라우팅 구조 확인
5. API 명세서에서 음악/배경음악 관련 항목 확인

### 2단계. 백엔드 구현

1. 관리자 UseCase/DTO 추가
2. 관리자 Controller 추가
3. Service에서 등록/수정/발행/숨김 처리
4. Repository 조회/저장 메서드 추가
5. 권한 검사 적용
6. 필요 시 감사 로그 추가
7. Controller/Service 테스트 추가

### 3단계. 관리자 웹 구현

1. `admin-web/src/api/musicTracks.ts` 추가
2. 메뉴에 `배경음악 관리` 추가
3. `MusicTracksPage.tsx` 추가
4. 목록 테이블 구현
5. 등록/수정 모달 구현
6. 발행/숨김 액션 구현
7. 상태/에러/로딩 처리

### 4단계. 문서 반영

1. OpenAPI 명세 추가
2. API 명세서 업데이트
3. 작업 리포트 작성
4. 테스트 방법 기록

## 7. 테스트 방법

### 백엔드

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*Music*"
.\qtai-server\gradlew.bat -p qtai-server :service-bible:test --tests "*Music*"
```

확인 항목:
- 관리자만 API 접근 가능
- 일반 사용자는 접근 불가
- 등록 성공
- 수정 성공
- ACTIVE/HIDDEN 상태 전이 정상
- 없는 ID 요청 시 404
- 잘못된 상태 전이 시 409 또는 INVALID_STATUS_TRANSITION

### 관리자 웹

```powershell
cd admin-web
npm.cmd test
npm.cmd run typecheck
npm.cmd run dev
```

확인 항목:
- 메뉴에 배경음악 관리 표시
- 목록 조회 가능
- 등록 가능
- 수정 가능
- 발행 가능
- 숨김 가능
- 성공/실패 메시지 정상 표시

## 8. 수동 확인 시나리오

1. 관리자 웹 접속
   - `http://localhost:5173`
2. `배경음악 관리` 메뉴 이동
3. 배경음악 등록
   - 제목
   - 아티스트
   - 음원 URL 또는 파일 식별자
   - 라이선스 메모
4. 등록된 항목이 목록에 표시되는지 확인
5. 수정 버튼으로 내용 변경
6. 발행 버튼으로 ACTIVE 전환
7. 숨김 버튼으로 HIDDEN 전환
8. 새로고침 후 상태 유지 확인

## 9. 완료 기준

- 관리자 웹에서 배경음악 목록을 볼 수 있다.
- 관리자 웹에서 배경음악을 등록할 수 있다.
- 관리자 웹에서 배경음악을 수정할 수 있다.
- 관리자 웹에서 배경음악을 발행/숨김 처리할 수 있다.
- 관리자 API는 OPERATOR/SUPER_ADMIN 권한으로 보호된다.
- 사용자 앱 신규 구현 없이 기존 앱 흐름에는 영향이 없다.
- 관련 테스트와 문서가 추가된다.

## 10. 확인 필요 사항

- 음원 파일 업로드 방식인지 URL 저장 방식인지
- `music_tracks`를 쓸지 `praise_songs`를 확장할지
- 라이선스/출처 표기 필수 여부
- 감사 로그 기록 필요 여부
- ACTIVE 상태를 하나만 허용할지 여러 개 허용할지
