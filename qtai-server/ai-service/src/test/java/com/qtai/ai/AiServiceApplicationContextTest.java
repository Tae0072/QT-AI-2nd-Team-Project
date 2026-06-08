package com.qtai.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AiServiceApplication.class)
@ActiveProfiles("test")
class AiServiceApplicationContextTest {

    @Test
    void contextLoadsWithDefaultMockClientMode() {
        // Context load is the assertion.
    }
}
