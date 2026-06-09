package com.qtai.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/** lib-common 모듈 빌드/테스트 파이프라인 스모크. */
class LibCommonModuleTest {

    @Test
    void moduleClassLoads() {
        assertNotNull(LibCommonModule.class);
    }
}
