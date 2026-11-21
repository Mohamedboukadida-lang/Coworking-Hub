package com.coworkinghub.coworkinghub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared request helpers for the API integration tests. */
abstract class ApiTestSupport {

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

    protected long createSpace() throws Exception {
        return json(postJson("/api/coworking-spaces/", """
                {"name":"Hub Lac","address":"Les Berges du Lac, Tunis","coordinateX":36.83,"coordinateY":10.23}
                """), 200).get("id").asLong();
    }

    protected long addRoom(long spaceId, int capacity, double pricePerPersonPerHour) throws Exception {
        return json(postJson("/api/coworking-spaces/{id}/rooms",
                "{\"capacity\":%d,\"pricePerPersonPerHour\":%s}".formatted(capacity, pricePerPersonPerHour),
                spaceId), 201).get("id").asLong();
    }

    /** A space with one 4-person room at 10 per person per hour. */
    protected long createBookableSpace() throws Exception {
        long spaceId = createSpace();
        addRoom(spaceId, 4, 10.0);
        return spaceId;
    }

    protected static String reservationBody(long spaceId, int hours, int people, String extraJson) {
        return """
                {"date":"2026-12-01","time":"09:00","hours":%d,"numberOfPeople":%d,"coworkingSpaceId":%d%s}
                """.formatted(hours, people, spaceId, extraJson);
    }

    protected String registerAndGetToken(String email, String type) throws Exception {
        return json(postJson("/api/auth/register", """
                {"name":"Amira","surname":"Ben Ali","email":"%s","password":"s3cret-pass","typeCompte":"%s"}
                """.formatted(email, type)), 201).get("token").asText();
    }
}
