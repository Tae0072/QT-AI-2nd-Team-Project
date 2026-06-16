# dev-up.ps1 — 여러 기기 동시 갱신(-AllDevices) (2026-06-15)

## 1. 문제
실물 태블릿과 에뮬레이터를 둘 다 연결한 채 `dev-up.ps1`을 돌렸는데 두 기기가 **다른 화면**(한쪽이 구버전)으로 보임.

## 2. 원인
- `dev-up.ps1`의 마지막 단계는 `flutter run -d <기기>`로, **기기 하나에만** 최신 빌드를 설치·실행한다.
  기기 자동선택은 "실기기 → 에뮬레이터 → AVD → 웹" 순서라, 둘 다 연결되면 보통 **실기기 하나만** 갱신되고
  에뮬레이터는 **이전에 설치된 구버전 APK**가 그대로 남는다 → 화면이 달라 보임.
- (참고) 실기기 자체는 기존에도 `adb reverse tcp:8080`로 백엔드에 정상 연결되고 있었다. 즉 10.0.2.2 문제는
  USB 실기기에는 해당 없음(에뮬레이터 전용 주소). 핵심 원인은 "한 기기만 재설치"였다.

## 3. 조치
`scripts/dev-up.ps1`에 **`-AllDevices`** 스위치 추가.
- 기기별 백엔드 주소 산출 로직을 `Get-DeviceDefines` 함수로 추출(웹/에뮬레이터=기본값, 실기기=adb reverse + DEV_BASE_URL=localhost).
  여러 기기 연결 대비 `adb -s <id>`를 사용하도록 보정.
- `-AllDevices`면 주(主) 기기(`flutter run` 대상)를 제외한 **모든 안드로이드 기기**에
  같은 코드를 기기 종류에 맞는 설정으로 `flutter build apk --debug` → `adb -s <id> install -r` → monkey로 실행한다.
- 기본 동작(스위치 미지정)은 기존과 동일 — 호환성 보존.

## 4. 사용법
```powershell
# 에뮬레이터 + 실물 태블릿을 모두 최신으로 맞추기(주 기기는 flutter run으로 hot reload)
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1 -AllDevices

# 특정 기기를 주 기기로 지정 + 나머지도 갱신
powershell -ExecutionPolicy Bypass -File .\scripts\dev-up.ps1 -AllDevices -Device <기기ID>
```
- 기기 종류별 백엔드: 에뮬레이터=10.0.2.2, 실기기(USB)=adb reverse+localhost로 자동 처리.
- 빌드가 기기 수만큼 반복되어 시간이 더 걸린다(정확한 설정을 위해 기기별 빌드).

## 5. 검증
- PowerShell 파서(`[Parser]::ParseFile`)로 구문 검증 통과.
- 기본 동작 경로(스위치 미지정)는 변경 없음. CI는 PS 스크립트를 실행하지 않음.

## 6. 한계/후속
- 실기기를 Wi-Fi(무선 디버깅)로 연결하는 경우 adb reverse 대신 PC LAN IP로 `DEV_BASE_URL`을 줘야 할 수 있음(후속 옵션 검토).
