package com.banking.auth.service;

import com.banking.auth.dto.*;
import com.banking.auth.entity.RefreshToken;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.RoleType;
import com.banking.auth.entity.User;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.CustomUserDetails;
import com.banking.auth.security.JwtTokenProvider;
import com.banking.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role(1L, RoleType.ROLE_CUSTOMER);
        sampleUser = new User("john_doe", "john@example.com", "encodedPassword", "John", "Doe");
        sampleUser.setId(10L);
        sampleUser.setRoles(Set.of(customerRole));
    }

    @Test
    @DisplayName("Should successfully register a new user with hashed password")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "password123", "John", "Doe", "ROLE_CUSTOMER");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName(RoleType.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserSummaryDto response = authService.register(request);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("ROLE_CUSTOMER"));
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when registering existing username")
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "password123", "John", "Doe", null);

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should authenticate and return JWT token pair on valid login")
    void login_Success() {
        LoginRequest request = new LoginRequest("john_doe", "password123");
        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(userDetails)).thenReturn("jwt.access.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));

        RefreshToken refreshToken = new RefreshToken(sampleUser, "random-refresh-token", Instant.now().plusSeconds(3600));
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt.access.token", response.getAccessToken());
        assertEquals("random-refresh-token", response.getRefreshToken());
        assertEquals("john_doe", response.getUsername());
        assertEquals(10L, response.getUserId());
        assertTrue(response.getRoles().contains("ROLE_CUSTOMER"));
    }
}
