package com.banking.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiGatewayApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Gateway context loads and Actuator health endpoint responds")
    void contextLoadsAndHealthCheckResponds() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    @DisplayName("Gateway preserves incoming X-Correlation-Id header")
    void preservesIncomingCorrelationId() throws Exception {
        String testCorrId = "client-custom-corr-12345";
        mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-Id", testCorrId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", testCorrId));
    }
}
