package com.banking.audit.controller;

import com.banking.audit.entity.AuditLog;
import com.banking.audit.repository.AuditLogRepository;
import com.banking.common.security.JwtTokenParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockBean
    private JwtTokenParser jwtTokenParser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        when(jwtTokenParser.validateToken(anyString())).thenReturn(true);
        when(jwtTokenParser.getUserId(anyString())).thenReturn(1L);
        when(jwtTokenParser.getUsername(anyString())).thenReturn("admin_user");
        when(jwtTokenParser.getEmail(anyString())).thenReturn("admin@banking.com");
        when(jwtTokenParser.getRoles(anyString())).thenReturn(List.of("ROLE_ADMIN"));

        AuditLog log = new AuditLog(
                "corr-test-01",
                "admin_user",
                "UPDATE_CUSTOMER_STATUS",
                "CUSTOMER",
                "CUST-100200",
                "SUCCESS",
                "192.168.1.1",
                "{\"status\":\"ACTIVE\"}"
        );
        auditLogRepository.save(log);
    }

    @Test
    @DisplayName("Should search audit logs as ADMIN user")
    void searchLogs_AsAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .header("Authorization", "Bearer valid.admin.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].actor").value("admin_user"))
                .andExpect(jsonPath("$.data.content[0].action").value("UPDATE_CUSTOMER_STATUS"));
    }

    @Test
    @DisplayName("Should retrieve resource history as ADMIN user")
    void getResourceHistory_Success() throws Exception {
        mockMvc.perform(get("/api/audit/resource/CUSTOMER/CUST-100200")
                        .header("Authorization", "Bearer valid.admin.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].resourceId").value("CUST-100200"));
    }
}
