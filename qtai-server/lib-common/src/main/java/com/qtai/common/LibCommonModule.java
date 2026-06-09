package com.qtai.common;

/**
 * lib-common 모듈 마커.
 *
 * <p>이 모듈은 모든 MSA 서비스가 공유하는 공통 코드를 담는다.
 * ②단계에서 다음을 추가한다:
 * <ul>
 *   <li>공통 응답 포맷 / 공통 예외 처리</li>
 *   <li>JWT 검증 필터 (유저 서비스가 발급한 토큰을 각 서비스가 검증)</li>
 *   <li>서비스 간 호출용 RestClient 설정</li>
 * </ul>
 */
public final class LibCommonModule {

    private LibCommonModule() {
    }
}
