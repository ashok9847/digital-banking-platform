package com.banking.auth.controller;

import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.dto.TokenRefreshRequest;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.RoleType;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setupDatabase() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(new Role(RoleType.ROLE_CUSTOMER));
        roleRepository.save(new Role(RoleType.ROLE_ADMIN));
        roleRepository.save(new Role(RoleType.ROLE_SUPPORT));
    }

    @Test
    @DisplayName("Complete Auth flow: Register -> Login -> Access /me -> Refresh -> Logout")
    void fullAuthFlowIntegrationTest() throws Exception {
        // 1. Register
        RegisterRequest registerReq = new RegisterRequest(
                "alex_miller",
                "alex@banking.com",
                "Password@123",
                "Alex",
                "Miller",
                "ROLE_CUSTOMER"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alex_miller"))
                .andExpect(jsonPath("$.data.email").value("alex@banking.com"))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CUSTOMER")));

        // 2. Duplicate Registration should return 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));

        // 3. Login with invalid password returns 401 Unauthorized
        LoginRequest badLogin = new LoginRequest("alex_miller", "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        // 4. Successful Login
        LoginRequest validLogin = new LoginRequest("alex_miller", "Password@123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("data").get("accessToken").asText();
        String refreshToken = loginJson.get("data").get("refreshToken").asText();

        // 5. Access protected /api/auth/me without token returns 401
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // 6. Access protected /api/auth/me with Bearer token returns 200
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alex_miller"))
                .andExpect(jsonPath("$.data.email").value("alex@banking.com"));

        // 7. Refresh token
        TokenRefreshRequest refreshReq = new TokenRefreshRequest(refreshToken);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }
}
