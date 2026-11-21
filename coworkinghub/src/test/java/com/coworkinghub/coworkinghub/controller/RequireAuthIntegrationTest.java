package com.coworkinghub.coworkinghub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With app.security.require-auth=true, only registration and login work without a token. */
@SpringBootTest(properties = {
        "app.security.require-auth=true",
        "spring.datasource.url=jdbc:h2:mem:coworkinghub-require-auth;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class RequireAuthIntegrationTest extends ApiTestSupport {

    @Test
    void endpointsRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/coworking-spaces")).andExpect(status().isUnauthorized());
        mockMvc.perform(postJson("/api/users/", "{\"name\":\"Anon\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationWorksAnonymouslyAndTokenUnlocksEndpoints() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "USER");
        mockMvc.perform(withToken(get("/api/coworking-spaces"), token)).andExpect(status().isOk());
    }
}
