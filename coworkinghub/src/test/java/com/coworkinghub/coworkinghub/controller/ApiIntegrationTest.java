package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.model.Equipment;
import com.coworkinghub.coworkinghub.repository.EquipmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest extends ApiTestSupport {

    @Autowired
    private EquipmentRepository equipmentRepository;

    // ---- users ----

    @Test
    void createUserReturnsPersistedAccountWithoutPassword() throws Exception {
        mockMvc.perform(postJson("/api/users/", """
                        {"name":"Amira","surname":"Ben Ali","age":28,"profession":"Designer",
                         "typeCompte":"OWNER","password":"hidden-value"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Amira"))
                .andExpect(jsonPath("$.typeCompte").value("OWNER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createEndpointsAcceptPathWithoutTrailingSlash() throws Exception {
        mockMvc.perform(postJson("/api/users", "{\"name\":\"NoSlash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NoSlash"));
    }

    @Test
    void getAndUpdateUser() throws Exception {
        long id = json(postJson("/api/users/", "{\"name\":\"Before\",\"age\":30}"), 200).get("id").asLong();

        mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"After\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.age").value(30));
        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After"));
    }

    @Test
    void unknownUserReturns404() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999_999)).andExpect(status().isNotFound());
    }

    // ---- coworking spaces ----

    @Test
    void createCoworkingSpaceRoundTripsFields() throws Exception {
        mockMvc.perform(postJson("/api/coworking-spaces/", """
                        {"name":"Hub Lac","address":"Les Berges du Lac, Tunis","coordinateX":36.83,"coordinateY":10.23}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Hub Lac"))
                .andExpect(jsonPath("$.address").value("Les Berges du Lac, Tunis"))
                .andExpect(jsonPath("$.coordinateX").value(36.83))
                .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    void listGetUpdateAndDeleteSpace() throws Exception {
        long id = createSpace();

        JsonNode all = json(get("/api/coworking-spaces"), 200);
        assertThat(all.findValuesAsText("id")).contains(String.valueOf(id));

        mockMvc.perform(put("/api/coworking-spaces/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.address").value("Les Berges du Lac, Tunis"));

        mockMvc.perform(delete("/api/coworking-spaces/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/coworking-spaces/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deletingSpaceThatStillHasRoomsReturns409() throws Exception {
        long id = createBookableSpace();
        mockMvc.perform(delete("/api/coworking-spaces/{id}", id)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/coworking-spaces/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void roomsCanBeAddedAndListed() throws Exception {
        long spaceId = createSpace();
        addRoom(spaceId, 2, 5.5);
        addRoom(spaceId, 8, 4.0);

        mockMvc.perform(get("/api/coworking-spaces/{id}/rooms", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void roomWithZeroCapacityIsRejected() throws Exception {
        long spaceId = createSpace();
        mockMvc.perform(postJson("/api/coworking-spaces/{id}/rooms",
                        "{\"capacity\":0,\"pricePerPersonPerHour\":5}", spaceId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addRandomEquipmentCreatesOneItemPerTypeAndListsThem() throws Exception {
        long spaceId = createSpace();

        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId))
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(content.getResponse().getContentAsString())
                        .contains("ID: " + spaceId));

        mockMvc.perform(get("/api/coworking-spaces/{id}/equipment", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void addRandomEquipmentToUnknownSpaceReturns404() throws Exception {
        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("999999")));
    }

    // ---- reservations ----

    @Test
    void reservationIsPricedWithCheapestFittingRoomAndEquipment() throws Exception {
        long spaceId = createSpace();
        addRoom(spaceId, 1, 2.0);   // too small for 2 people
        addRoom(spaceId, 6, 12.0);  // fits, more expensive
        long cheapRoom = addRoom(spaceId, 4, 10.0);  // fits, cheapest
        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId))
                .andExpect(status().isOk());
        List<Equipment> equipment = equipmentRepository.findByCoworkingSpaceId(spaceId);
        Equipment first = equipment.get(0);
        Equipment second = equipment.get(1);

        // (10 x 2 people + equipment per hour) x 3 hours
        double expected = Math.round((10.0 * 2 + first.getPrice() + second.getPrice()) * 3 * 100) / 100.0;

        mockMvc.perform(postJson("/api/reservations/", reservationBody(spaceId, 3, 2,
                        ",\"equipmentIds\":[%d,%d]".formatted(first.getId(), second.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.roomId").value(cheapRoom))
                .andExpect(jsonPath("$.coworkingSpaceId").value(spaceId))
                .andExpect(jsonPath("$.totalPrice").value(expected));
    }

    @Test
    void reservationWithoutEquipmentIsPricedOnRoomOnly() throws Exception {
        long spaceId = createBookableSpace();
        mockMvc.perform(postJson("/api/reservations/", reservationBody(spaceId, 2, 3, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(60.0))
                .andExpect(jsonPath("$.date").value("2026-12-01"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void reservationLargerThanEveryRoomReturns400() throws Exception {
        long spaceId = createBookableSpace();
        mockMvc.perform(postJson("/api/reservations/", reservationBody(spaceId, 1, 5, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("can hold 5 people")));
    }

    @Test
    void reservationInSpaceWithoutRoomsReturns400() throws Exception {
        long spaceId = createSpace();
        mockMvc.perform(postJson("/api/reservations/", reservationBody(spaceId, 1, 1, "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void equipmentFromAnotherSpaceIsRejected() throws Exception {
        long spaceId = createBookableSpace();
        long otherSpace = createSpace();
        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", otherSpace))
                .andExpect(status().isOk());
        long foreignEquipment = equipmentRepository.findByCoworkingSpaceId(otherSpace).get(0).getId();

        mockMvc.perform(postJson("/api/reservations/", reservationBody(spaceId, 1, 1,
                        ",\"equipmentIds\":[" + foreignEquipment + "]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("does not belong")));
    }

    @Test
    void reservationForUnknownSpaceReturns404() throws Exception {
        mockMvc.perform(postJson("/api/reservations/", reservationBody(999_999, 1, 1, "")))
                .andExpect(status().isNotFound());
    }

    @Test
    void reservationWithUnknownEquipmentReturns404() throws Exception {
        long spaceId = createBookableSpace();
        mockMvc.perform(postJson("/api/reservations/", reservationBody(spaceId, 1, 1, ",\"equipmentIds\":[999999]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Equipment")));
    }

    @Test
    void reservationMissingRequiredFieldsReturns400() throws Exception {
        mockMvc.perform(postJson("/api/reservations/", "{\"hours\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("coworkingSpaceId")))
                .andExpect(jsonPath("$.detail", containsString("numberOfPeople")));
    }

    @Test
    void reservationCanBeReadListedUpdatedAndDeleted() throws Exception {
        long spaceId = createBookableSpace();
        long id = json(postJson("/api/reservations/", reservationBody(spaceId, 1, 1, "")), 200).get("id").asLong();

        mockMvc.perform(get("/api/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.date").value("2026-12-01"));
        mockMvc.perform(get("/api/reservations").param("coworkingSpaceId", String.valueOf(spaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(put("/api/reservations/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DECLINED\",\"reason\":\"Fully booked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.statusChangeReason").value("Fully booked"));

        mockMvc.perform(delete("/api/reservations/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/reservations/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deletingReservationWithEquipmentAlsoRemovesLinks() throws Exception {
        long spaceId = createBookableSpace();
        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId))
                .andExpect(status().isOk());
        long equipmentId = equipmentRepository.findByCoworkingSpaceId(spaceId).get(0).getId();
        long id = json(postJson("/api/reservations/",
                reservationBody(spaceId, 1, 1, ",\"equipmentIds\":[" + equipmentId + "]")), 200).get("id").asLong();

        mockMvc.perform(delete("/api/reservations/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void updateStatusOfUnknownReservationReturns404() throws Exception {
        mockMvc.perform(put("/api/reservations/{id}/status", 999_999).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatusWithoutStatusReturns400() throws Exception {
        mockMvc.perform(put("/api/reservations/{id}/status", 1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"missing status\"}"))
                .andExpect(status().isBadRequest());
    }
}
