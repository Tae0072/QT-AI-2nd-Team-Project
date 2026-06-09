package com.qtai.domain.qt.internal;

/**
 * 일자별 QT 스냅샷(JSON)을 오브젝트 스토리지에 저장하는 포트.
 *
 * <p>구현 어댑터는 {@code qt.client.objectstorage}에 둔다(CLAUDE.md §4: 도메인 전용 외부 시스템
 * 호출은 client). 기본은 로컬 파일 어댑터, 배포는 S3 호환(S3/R2/MinIO) 어댑터가 동작한다.
 * 앱(Flutter)은 이렇게 생성된 날짜별 JSON만 읽어 표시하므로 서버 부하·인증이 불필요하다
 * (회의록 2026-06-09 §2).
 */
public interface QtSnapshotStore {

    /**
     * 스냅샷 JSON을 주어진 오브젝트 키(예: {@code 2026-06-09.json})로 저장한다.
     *
     * @return 저장 위치(로컬 경로 또는 {@code s3://bucket/key} 형태). 로그·응답에 키 외 민감정보는 담지 않는다.
     */
    String store(String objectKey, String json);
}
