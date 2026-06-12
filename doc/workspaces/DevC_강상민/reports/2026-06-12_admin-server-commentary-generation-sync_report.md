# Report - 2026-06-12 admin-server 주석자료 생성 경로 동기화

## 요약

AI 해설 산출물 생성 시 `service-ai`는 주석자료를 프롬프트에 넣고 `payloadJson.sourceMetadata`에 주석자료 출처 메타데이터를 남기지만, `admin-server` 복사본은 해당 로직이 빠져 있었다.

`admin-server`에서도 같은 대상의 verseIds 기준으로 주석자료를 조회하고, 프롬프트 및 산출물 metadata에 반영하도록 동기화했다.

## 확인한 근거

- `service-ai`의 `ExplanationGenerationJobHandler`는 `CommentaryMaterialService`를 주입받는다.
- `service-ai`는 `sourceMetadata`에 `commentarySource`, `sourceName`, `licenseLabel`, `copyrightNotice`, `commentaryMaterialIds`, `commentaryVerseRange`를 저장한다.
- `admin-server`에는 `CommentaryMaterialService`, `CommentaryMaterialContext`, 관련 entity/repository/status enum이 없었다.
- `admin-server`의 `ExplanationGenerationJobHandler`는 verse metadata만 payload에 저장하고 주석자료를 prompt에 넣지 않았다.

## 변경 내용

- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterialContext.java`
  - 주석자료 프롬프트 컨텍스트 DTO를 추가했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterialService.java`
  - verseIds 기준 active generation input 주석자료를 조회하고 excerpt를 구성한다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentarySource.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterial.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterialVerse.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterialVerseRepository.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentarySourceStatus.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/CommentaryMaterialStatus.java`
  - service-ai와 동일한 주석자료 조회 모델을 추가했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/ExplanationGenerationJobHandler.java`
  - `CommentaryMaterialService` 주입을 추가했다.
  - QT_PASSAGE/BIBLE_VERSE 입력 생성 시 주석자료 컨텍스트를 조회한다.
  - user prompt에 주석자료 excerpt를 포함한다.
  - payload `sourceMetadata`에 주석자료 출처 metadata를 포함한다.

## 제외한 내용

- 주석자료 원문 전체를 산출물 payload에 저장하지 않았다.
- service-ai 코드는 변경하지 않았다.
- DB schema 수동 변경은 하지 않았다. 로컬 compose는 기존 `update` 설정을 따른다.
- 관리자 상세 UI의 전용 주석자료 섹션은 추가하지 않았다.

## 검증 결과

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:compileJava
```

결과: 성공

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:bootJar
```

결과: 성공

```powershell
docker compose up -d --build service-admin
```

결과: 성공

컨테이너 상태:

- `qtai-admin-server`: `healthy`

## 후속 검토

- service-ai/admin-server 복사본 중복 때문에 생성 로직 drift가 재발할 수 있다.
- 상세 화면에서 `Payload JSON` raw 대신 주석자료 metadata를 별도 행으로 노출하면 운영자 확인성이 좋아진다.
