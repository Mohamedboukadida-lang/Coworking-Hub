package com.coworkinghub.coworkinghub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared request helpers for the API integration tests. */
abstract class ApiTestSupport {

    protected static final String ADMIN_EMAIL = "admin@coworkinghub.test";
    protected static final String ADMIN_PASSWORD = "admin-pass-123";
    protected static final String DATE = "2027-03-01";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected JsonNode json(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String body = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : objectMapper.readTree(body);
    }

    protected static MockHttpServletRequestBuilder postJson(String url, String body, Object... vars) {
        return post(url, vars).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected String registerAndGetToken(String email, String type) throws Exception {
        return json(postJson("/api/auth/register", """
                {"name":"Amira","surname":"Ben Ali","email":"%s","password":"s3cret-pass","typeCompte":"%s"}
                """.formatted(email, type)), 201).get("token").asText();
    }

    protected String newOwner() throws Exception {
        return registerAndGetToken(uniqueEmail(), "OWNER");
    }

    protected String newMember() throws Exception {
        return registerAndGetToken(uniqueEmail(), "USER");
    }

    protected String adminToken() throws Exception {
        return json(postJson("/api/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD)), 200)
                .get("token").asText();
    }

    protected long idOf(String token) throws Exception {
        return json(withToken(get("/api/users/me"), token), 200).get("id").asLong();
    }

    protected long createSpace(String ownerToken) throws Exception {
        return json(withToken(postJson("/api/coworking-spaces/", """
                {"name":"Hub Lac","address":"Les Berges du Lac, Tunis","coordinateX":36.83,"coordinateY":10.23}
                """), ownerToken), 200).get("id").asLong();
    }

    protected long addRoom(String ownerToken, long spaceId, int capacity, double pricePerPersonPerHour) throws Exception {
        return json(withToken(postJson("/api/coworking-spaces/{id}/rooms",
                "{\"capacity\":%d,\"pricePerPersonPerHour\":%s}".formatted(capacity, pricePerPersonPerHour),
                spaceId), ownerToken), 201).get("id").asLong();
    }

    /** A space owned by {@code ownerToken} with one 4-person room at 10 per person per hour. */
    protected long createBookableSpace(String ownerToken) throws Exception {
        long spaceId = createSpace(ownerToken);
        addRoom(ownerToken, spaceId, 4, 10.0);
        return spaceId;
    }

    protected static String booking(long spaceId, String time, int hours, int people, String extraJson) {
        return """
                {"date":"%s","time":"%s","hours":%d,"numberOfPeople":%d,"coworkingSpaceId":%d%s}
                """.formatted(DATE, time, hours, people, spaceId, extraJson);
    }

    protected static String booking(long spaceId, int hours, int people, String extraJson) {
        return booking(spaceId, "09:00", hours, people, extraJson);
    }

    protected MockHttpServletRequestBuilder book(String token, String body) {
        return withToken(postJson("/api/reservations/", body), token);
    }
}
