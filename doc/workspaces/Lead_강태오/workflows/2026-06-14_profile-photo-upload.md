# 2026-06-14 프로필 사진 변경(프론트+백엔드)

## 결정
- 사진은 **서버 DB에 저장**(음악 audio_data와 동일한 LONGBLOB + 스트림 패턴). 외부 CDN/S3 미사용.

## 현황 진단
- 백엔드 members에는 `profile_image_url`(문자열)만 있고 카카오 로그인 URL을 그대로 쓰는 구조 → 사진 업로드/저장 경로가 없었음. 그래서 업로드+스트림을 신설.

## 백엔드(service-user, 스키마=admin-server)
- 마이그레이션 `V40__add_member_profile_image_blob.sql`: members에 `profile_image_data(LONGBLOB)`, `profile_image_content_type`, `profile_image_updated_at` 추가. (admin-server는 validate라 추가 컬럼 무해 → admin Java 변경 불필요)
- `Member` 엔티티(service-user): BLOB 필드 + `updateProfilePhoto/clearProfilePhoto/hasProfilePhoto`. 업로드 시 `profile_image_url`을 `/api/v1/me/profile-photo?v={epoch}`(캐시버스트)로 채움.
- 신규 UseCase `UpdateProfilePhotoUseCase`/`GetProfilePhotoUseCase` + dto `ProfilePhotoView`, `MemberService` 구현(형식 jpeg/png/webp·5MB 검증).
- `MemberController`: `POST/DELETE/GET /api/v1/me/profile-photo`(multipart 업로드·삭제·바이트 스트림). `/me/**`라 인증 보호. multipart 최대 6MB 설정.
- 단위테스트: 업로드 성공/형식·크기 거부/미존재 404/삭제(MemberServiceTest).

## 프론트(Flutter)
- `image_picker` 추가(iOS Info.plist 사진/카메라 권한 문구). 
- `MyPageRepository`: `uploadProfilePhoto(bytes)`(Dio multipart), `deleteProfilePhoto()`, `getMyProfilePhotoBytes()`.
- `profile_edit_screen`: '사진 변경'(갤러리 선택→업로드)·'기본 이미지로'(삭제) 버튼. 표시는 카카오 http URL=NetworkImage, 우리 업로드분=Dio 바이트→MemoryImage.

## 검증
- Flutter: `flutter analyze`(mypage 무이슈) + `flutter test` **302개 통과**.
- 백엔드: 로컬 gradle은 Windows 파일락(데몬)으로 불안정 → 깨끗한 CI(qtai-server Build & Test, Linux)로 검증. 코드는 음악/닉네임 기존 패턴을 미러.
- 기기 실제 업로드/표시는 단말에서 확인 필요.

## 한계/후속
- mypage 상단 아바타(generic 아이콘)는 이번에 미변경(프로필 편집 화면 중심). 추후 동일 표시 로직 적용 가능.

## Git/PR
- 브랜치 `feature/profile-photo-upload` → PR 대상 `dev`.
