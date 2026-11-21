package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.model.Equipment;
import com.coworkinghub.coworkinghub.repository.EquipmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private long createSpace() throws Exception {
        String body = mockMvc.perform(post("/api/coworking-spaces/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hub Lac","address":"Les Berges du Lac, Tunis",
                                 "coordinateX":36.83,"coordinateY":10.23}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hub Lac"))
                .andExpect(jsonPath("$.address").value("Les Berges du Lac, Tunis"))
                .andExpect(jsonPath("$.coordinateX").value(36.83))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("id").isNumber()).isTrue();
        return json.get("id").asLong();
    }

    private long createReservation(long spaceId, String equipmentIdsJson) throws Exception {
        String body = mockMvc.perform(post("/api/reservations/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-12-01","time":"09:00","hours":3,"numberOfPeople":2,
                                 "coworkingSpaceId":%d%s}
                                """.formatted(spaceId, equipmentIdsJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.hours").value(3))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void createUserReturnsPersistedAccount() throws Exception {
        mockMvc.perform(post("/api/users/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Amira","surname":"Ben Ali","age":28,
                                 "profession":"Designer","typeCompte":"OWNER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Amira"))
                .andExpect(jsonPath("$.typeCompte").value("OWNER"));
    }

    @Test
    void createEndpointsAcceptPathWithoutTrailingSlash() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NoSlash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NoSlash"));
    }

    @Test
    void createCoworkingSpaceRoundTripsFields() throws Exception {
        createSpace();
    }

    @Test
    void addRandomEquipmentCreatesOneItemPerType() throws Exception {
        long spaceId = createSpace();
        long before = equipmentRepository.count();

        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId))
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(content.getResponse().getContentAsString())
                        .contains("ID: " + spaceId));

        assertThat(equipmentRepository.count() - before).isEqualTo(6);
    }

    @Test
    void addRandomEquipmentToUnknownSpaceReturns404() throws Exception {
        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("999999")));
    }

    @Test
    void createReservationWithEquipment() throws Exception {
        long spaceId = createSpace();
        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId))
                .andExpect(status().isOk());
        List<Equipment> equipment = equipmentRepository.findAll();
        long equipmentId = equipment.get(equipment.size() - 1).getId();

        createReservation(spaceId, ",\"equipmentIds\":[" + equipmentId + "]");
    }

    @Test
    void createReservationWithoutEquipmentIds() throws Exception {
        createReservation(createSpace(), "");
    }

    @Test
    void createReservationForUnknownSpaceReturns404() throws Exception {
        mockMvc.perform(post("/api/reservations/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coworkingSpaceId\":999999,\"hours\":1,\"numberOfPeople\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReservationWithUnknownEquipmentReturns404() throws Exception {
        long spaceId = createSpace();
        mockMvc.perform(post("/api/reservations/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coworkingSpaceId\":%d,\"equipmentIds\":[999999]}".formatted(spaceId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Equipment")));
    }

    @Test
    void createReservationWithoutSpaceIdReturns400() throws Exception {
        mockMvc.perform(post("/api/reservations/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hours\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("coworkingSpaceId")));
    }

    @Test
    void updateReservationStatus() throws Exception {
        long reservationId = createReservation(createSpace(), "");

        mockMvc.perform(put("/api/reservations/{id}/status", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DECLINED\",\"reason\":\"Fully booked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.statusChangeReason").value("Fully booked"));
    }

    @Test
    void updateStatusOfUnknownReservationReturns404() throws Exception {
        mockMvc.perform(put("/api/reservations/{id}/status", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatusWithoutStatusReturns400() throws Exception {
        mockMvc.perform(put("/api/reservations/{id}/status", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"missing status\"}"))
                .andExpect(status().isBadRequest());
    }
}
