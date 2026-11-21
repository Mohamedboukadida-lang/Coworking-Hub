package com.coworkinghub.coworkinghub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest extends ApiTestSupport {

    @Test
    void registerReturnsTokenAndUserWithoutPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(postJson("/api/auth/register", """
                        {"name":"Sami","email":"%s","password":"long-enough","typeCompte":"OWNER"}
                        """.formatted(email.toUpperCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.typeCompte").value("OWNER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void registerDefaultsToUserType() throws Exception {
        mockMvc.perform(postJson("/api/auth/register",
                        "{\"name\":\"Sami\",\"email\":\"%s\",\"password\":\"long-enough\"}".formatted(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.typeCompte").value("USER"));
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email, "USER");
        mockMvc.perform(postJson("/api/auth/register",
                        "{\"name\":\"Again\",\"email\":\"%s\",\"password\":\"long-enough\"}".formatted(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerAsAdminIsRejected() throws Exception {
        mockMvc.perform(postJson("/api/auth/register", """
                        {"name":"Eve","email":"%s","password":"long-enough","typeCompte":"ADMIN"}
                        """.formatted(uniqueEmail())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerValidatesEmailAndPassword() throws Exception {
        mockMvc.perform(postJson("/api/auth/register",
                        "{\"name\":\"Sami\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("email")))
                .andExpect(jsonPath("$.detail", containsString("password")));
    }

    @Test
    void loginSucceedsWithCorrectPasswordOnly() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email, "USER");

        mockMvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"s3cret-pass\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
        mockMvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"wrong-pass\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"s3cret-pass\"}".formatted(uniqueEmail())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapAdminCanLogIn() throws Exception {
        String token = adminToken();
        mockMvc.perform(withToken(get("/api/users/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeCompte").value("ADMIN"))
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
    }

    @Test
    void adminCreatedAccountPasswordIsHashedSoLoginWorks() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(withToken(postJson("/api/users/",
                        "{\"name\":\"Staff\",\"email\":\"%s\",\"password\":\"staff-pass\"}".formatted(email)), adminToken()))
                .andExpect(status().isOk());
        mockMvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"staff-pass\"}".formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void meRequiresToken() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email, "USER");

        mockMvc.perform(withToken(get("/api/users/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservations/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejectedEvenOnPublicEndpoints() throws Exception {
        mockMvc.perform(withToken(get("/api/coworking-spaces"), "not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }
}
