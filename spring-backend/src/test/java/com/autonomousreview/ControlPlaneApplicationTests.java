package com.autonomousreview;

import com.autonomousreview.config.TestRepositoryMockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRepositoryMockConfig.class)
class ControlPlaneApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring ApplicationContext boots successfully with MongoDB & Security
    }
}
