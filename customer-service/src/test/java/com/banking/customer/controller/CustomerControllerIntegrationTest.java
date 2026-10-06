package com.banking.customer.controller;

import com.banking.common.security.JwtTokenParser;
import com.banking.customer.dto.CustomerCreateRequest;
import com.banking.customer.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @MockBean
    private JwtTokenParser jwtTokenParser;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();

        // Default mock for a valid token
        when(jwtTokenParser.validateToken(anyString())).thenReturn(true);
        when(jwtTokenParser.getUserId(anyString())).thenReturn(50L);
        when(jwtTokenParser.getUsername(anyString())).thenReturn("customer_user");
        when(jwtTokenParser.getEmail(anyString())).thenReturn("customer@banking.com");
        when(jwtTokenParser.getRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("Should create customer profile and retrieve by ID")
    void createAndGetCustomerIntegrationTest() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest(
                50L,
                "Sarah",
                "Connor",
                "sarah.connor@banking.com",
                "+15551234567",
                LocalDate.of(1985, 3, 20),
                "742 Evergreen Terrace, Springfield"
        );

        // 1. Create Customer
        String responseStr = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer valid.mock.jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Sarah"))
                .andExpect(jsonPath("$.data.email").value("sarah.connor@banking.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseStr).get("data").get("id").asLong();

        // 2. Retrieve Customer by ID
        mockMvc.perform(get("/api/customers/" + createdId)
                        .header("Authorization", "Bearer valid.mock.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdId))
                .andExpect(jsonPath("$.data.lastName").value("Connor"));
    }

    @Test
    @DisplayName("Should reject unauthenticated requests with 401/403")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isForbidden());
    }
}
