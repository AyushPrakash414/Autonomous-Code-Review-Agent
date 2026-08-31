package com.autonomousreview;

import com.autonomousreview.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ControlPlaneApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        // Verifies Spring ApplicationContext boots successfully with MongoDB & Security
    }
}
