package com.dahyvuun.payment_router.service;

import com.dahyvuun.payment_router.TestcontainersConfig;
import com.dahyvuun.payment_router.dto.AuthRequest;
import com.dahyvuun.payment_router.dto.AuthResponse;
import com.dahyvuun.payment_router.exception.DuplicateEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    // Helper: build AuthRequest using setter-style (no @AllArgsConstructor on DTO)
    private AuthRequest buildRequest(String email, String password) {
        try {
            AuthRequest req = new AuthRequest();
            setField(req, "email", email);
            setField(req, "password", password);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Register success - returns JWT token")
    void register_success_returnsJwtToken() {
        // given
        AuthRequest request = buildRequest("newuser@test.com", "password123");

        // when
        AuthResponse response = authService.register(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        // Verify JWT format: header.payload.signature
        assertThat(response.getToken().split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Register with duplicate email - throws DuplicateEmailException")
    void register_duplicateEmail_throwsException() {
        // given
        AuthRequest request = buildRequest("duplicate@test.com", "password123");
        authService.register(request);

        // when & then
        assertThatThrownBy(() -> authService.register(buildRequest("duplicate@test.com", "password123")))
            .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("Login success - returns JWT token")
    void login_success_returnsJwtToken() {
        // given
        authService.register(buildRequest("logintest@test.com", "password123"));

        // when
        AuthResponse response = authService.login(buildRequest("logintest@test.com", "password123"));

        // then
        assertThat(response.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("Login with wrong password - throws exception")
    void login_wrongPassword_throwsException() {
        // given
        authService.register(buildRequest("user@test.com", "correctpassword"));

        // when & then
        assertThatThrownBy(() ->
            authService.login(buildRequest("user@test.com", "wrongpassword")))
            .isInstanceOf(Exception.class);
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}