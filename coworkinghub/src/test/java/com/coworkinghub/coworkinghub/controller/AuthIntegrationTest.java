package com.coworkinghub.coworkinghub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Authentication with the API left open (the default): tokens are optional but honoured. */
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
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email));
        mockMvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"wrong-pass\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"s3cret-pass\"}".formatted(uniqueEmail())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void legacyCreateUserHashesPasswordSoLoginWorks() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(postJson("/api/users/",
                        "{\"name\":\"Legacy\",\"email\":\"%s\",\"password\":\"legacy-pass\"}".formatted(email)))
                .andExpect(status().isOk());
        mockMvc.perform(postJson("/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"legacy-pass\"}".formatted(email)))
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
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(withToken(get("/api/coworking-spaces"), "not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    @Test
    void authenticatedCallerOwnsSpaceAndReservation() throws Exception {
        String ownerToken = registerAndGetToken(uniqueEmail(), "OWNER");
        long ownerId = json(withToken(get("/api/users/me"), ownerToken), 200).get("id").asLong();

        long spaceId = json(withToken(postJson("/api/coworking-spaces/", "{\"name\":\"Owned\"}"), ownerToken), 200)
                .get("id").asLong();
        mockMvc.perform(get("/api/coworking-spaces/{id}", spaceId))
                .andExpect(jsonPath("$.ownerId").value(ownerId));
        addRoom(spaceId, 4, 10.0);

        String userToken = registerAndGetToken(uniqueEmail(), "USER");
        long userId = json(withToken(get("/api/users/me"), userToken), 200).get("id").asLong();
        long reservationId = json(withToken(postJson("/api/reservations/", reservationBody(spaceId, 1, 1, "")),
                userToken), 200).get("id").asLong();

        mockMvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(jsonPath("$.userId").value(userId));
        mockMvc.perform(withToken(get("/api/reservations/me"), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reservationId));
        mockMvc.perform(get("/api/reservations").param("userId", String.valueOf(userId)))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
