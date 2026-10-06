package com.banking.notification.controller;

import com.banking.common.security.JwtTokenParser;
import com.banking.notification.entity.Notification;
import com.banking.notification.entity.NotificationStatus;
import com.banking.notification.entity.NotificationType;
import com.banking.notification.repository.NotificationRepository;
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
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private JwtTokenParser jwtTokenParser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        when(jwtTokenParser.validateToken(anyString())).thenReturn(true);
        when(jwtTokenParser.getUserId(anyString())).thenReturn(10L);
        when(jwtTokenParser.getUsername(anyString())).thenReturn("notif_user");
        when(jwtTokenParser.getEmail(anyString())).thenReturn("notif@banking.com");
        when(jwtTokenParser.getRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));

        Notification notif = new Notification(
                10L,
                "TXN-999000",
                NotificationType.EMAIL,
                "customer@banking.com",
                "Deposit Confirmed",
                "Your deposit of $500 was completed",
                NotificationStatus.SENT
        );
        notificationRepository.save(notif);
    }

    @Test
    @DisplayName("Should retrieve notification history by customer ID")
    void getNotificationsByCustomerId_IntegrationTest() throws Exception {
        mockMvc.perform(get("/api/notifications/customer/10")
                        .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].transactionReference").value("TXN-999000"))
                .andExpect(jsonPath("$.data[0].status").value("SENT"));
    }
}
