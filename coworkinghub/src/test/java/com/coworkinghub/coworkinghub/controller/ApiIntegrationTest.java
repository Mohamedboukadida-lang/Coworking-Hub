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

/** Happy paths and input validation, each performed by an account that is allowed to do it. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest extends ApiTestSupport {

    @Autowired
    private EquipmentRepository equipmentRepository;

    // ---- users ----

    @Test
    void adminCanCreateAccountsAndPasswordIsNeverReturned() throws Exception {
        mockMvc.perform(withToken(postJson("/api/users/", """
                        {"name":"Amira","surname":"Ben Ali","age":28,"profession":"Designer",
                         "typeCompte":"OWNER","password":"hidden-value"}
                        """), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.typeCompte").value("OWNER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createEndpointsAcceptPathWithoutTrailingSlash() throws Exception {
        mockMvc.perform(withToken(postJson("/api/users", "{\"name\":\"NoSlash\"}"), adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NoSlash"));
    }

    @Test
    void userCanReadAndUpdateOwnProfile() throws Exception {
        String token = newMember();
        long id = idOf(token);

        mockMvc.perform(withToken(put("/api/users/{id}", id), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"After\",\"age\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.surname").value("Ben Ali"));
        mockMvc.perform(withToken(get("/api/users/{id}", id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.email").isString());
    }

    @Test
    void unknownUserReturns404() throws Exception {
        mockMvc.perform(withToken(get("/api/users/{id}", 999_999), newMember())).andExpect(status().isNotFound());
    }

    // ---- coworking spaces ----

    @Test
    void ownerCreatesSpaceAndBecomesItsOwner() throws Exception {
        String owner = newOwner();
        mockMvc.perform(withToken(postJson("/api/coworking-spaces/", """
                        {"name":"Hub Lac","address":"Les Berges du Lac, Tunis","coordinateX":36.83,"coordinateY":10.23}
                        """), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Hub Lac"))
                .andExpect(jsonPath("$.coordinateX").value(36.83))
                .andExpect(jsonPath("$.ownerId").value(idOf(owner)));
    }

    @Test
    void catalogueIsPublic() throws Exception {
        String owner = newOwner();
        long id = createBookableSpace(owner);

        JsonNode all = json(get("/api/coworking-spaces"), 200);
        assertThat(all.findValuesAsText("id")).contains(String.valueOf(id));
        mockMvc.perform(get("/api/coworking-spaces/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/coworking-spaces/{id}/rooms", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/coworking-spaces/{id}/equipment", id)).andExpect(status().isOk());
    }

    @Test
    void ownerUpdatesAndDeletesSpace() throws Exception {
        String owner = newOwner();
        long id = createSpace(owner);

        mockMvc.perform(withToken(put("/api/coworking-spaces/{id}", id), owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.address").value("Les Berges du Lac, Tunis"));

        mockMvc.perform(withToken(delete("/api/coworking-spaces/{id}", id), owner)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/coworking-spaces/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deletingSpaceThatStillHasRoomsReturns409() throws Exception {
        String owner = newOwner();
        long id = createBookableSpace(owner);
        mockMvc.perform(withToken(delete("/api/coworking-spaces/{id}", id), owner)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/coworking-spaces/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void roomWithZeroCapacityIsRejected() throws Exception {
        String owner = newOwner();
        long spaceId = createSpace(owner);
        mockMvc.perform(withToken(postJson("/api/coworking-spaces/{id}/rooms",
                        "{\"capacity\":0,\"pricePerPersonPerHour\":5}", spaceId), owner))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerAddsRandomEquipmentOnePerType() throws Exception {
        String owner = newOwner();
        long spaceId = createSpace(owner);

        mockMvc.perform(withToken(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId), owner))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/coworking-spaces/{id}/equipment", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void addRandomEquipmentToUnknownSpaceReturns404() throws Exception {
        mockMvc.perform(withToken(post("/api/coworking-spaces/{id}/add-random-equipment", 999_999), newOwner()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("999999")));
    }

    // ---- reservations ----

    @Test
    void reservationIsPricedWithCheapestFittingRoomAndEquipment() throws Exception {
        String owner = newOwner();
        long spaceId = createSpace(owner);
        addRoom(owner, spaceId, 1, 2.0);                    // too small for 2 people
        addRoom(owner, spaceId, 6, 12.0);                   // fits, more expensive
        long cheapRoom = addRoom(owner, spaceId, 4, 10.0);  // fits, cheapest
        mockMvc.perform(withToken(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId), owner))
                .andExpect(status().isOk());
        List<Equipment> equipment = equipmentRepository.findByCoworkingSpaceId(spaceId);
        Equipment first = equipment.get(0);
        Equipment second = equipment.get(1);
        double expected = Math.round((10.0 * 2 + first.getPrice() + second.getPrice()) * 3 * 100) / 100.0;

        String member = newMember();
        mockMvc.perform(book(member, booking(spaceId, 3, 2,
                        ",\"equipmentIds\":[%d,%d]".formatted(first.getId(), second.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.roomId").value(cheapRoom))
                .andExpect(jsonPath("$.coworkingSpaceId").value(spaceId))
                .andExpect(jsonPath("$.userId").value(idOf(member)))
                .andExpect(jsonPath("$.date").value(DATE))
                .andExpect(jsonPath("$.totalPrice").value(expected));
    }

    @Test
    void timeIsNormalisedToHoursAndMinutes() throws Exception {
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(newMember(), booking(spaceId, "9:30", 2, 3, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time").value("09:30"))
                .andExpect(jsonPath("$.totalPrice").value(60.0));
    }

    @Test
    void reservationLargerThanEveryRoomReturns400() throws Exception {
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(newMember(), booking(spaceId, 1, 5, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("can hold 5 people")));
    }

    @Test
    void equipmentFromAnotherSpaceIsRejected() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        long otherSpace = createSpace(owner);
        mockMvc.perform(withToken(post("/api/coworking-spaces/{id}/add-random-equipment", otherSpace), owner))
                .andExpect(status().isOk());
        long foreignEquipment = equipmentRepository.findByCoworkingSpaceId(otherSpace).get(0).getId();

        mockMvc.perform(book(newMember(), booking(spaceId, 1, 1, ",\"equipmentIds\":[" + foreignEquipment + "]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("does not belong")));
    }

    @Test
    void reservationForUnknownSpaceOrEquipmentReturns404() throws Exception {
        String member = newMember();
        mockMvc.perform(book(member, booking(999_999, 1, 1, ""))).andExpect(status().isNotFound());
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(member, booking(spaceId, 1, 1, ",\"equipmentIds\":[999999]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Equipment")));
    }

    @Test
    void reservationValidatesRequiredFieldsAndTimeFormat() throws Exception {
        String member = newMember();
        mockMvc.perform(book(member, "{\"hours\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("coworkingSpaceId")))
                .andExpect(jsonPath("$.detail", containsString("date")))
                .andExpect(jsonPath("$.detail", containsString("time")));
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(member, booking(spaceId, "25:00", 1, 1, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("HH:mm")));
    }

    @Test
    void reservationMustEndByMidnight() throws Exception {
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(newMember(), booking(spaceId, "22:00", 3, 1, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("midnight")));
    }

    @Test
    void reservationLifecycle() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        String member = newMember();
        long id = json(book(member, booking(spaceId, 1, 1, "")), 200).get("id").asLong();

        mockMvc.perform(withToken(get("/api/reservations/{id}", id), member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(DATE));
        mockMvc.perform(withToken(get("/api/reservations/me"), member))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(withToken(get("/api/reservations").param("coworkingSpaceId", String.valueOf(spaceId)), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(withToken(put("/api/reservations/{id}/status", id), owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"reason\":\"See you\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.statusChangeReason").value("See you"));

        mockMvc.perform(withToken(delete("/api/reservations/{id}", id), member)).andExpect(status().isNoContent());
        mockMvc.perform(withToken(get("/api/reservations/{id}", id), member)).andExpect(status().isNotFound());
    }

    @Test
    void cancellingReservationWithEquipmentRemovesLinks() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        mockMvc.perform(withToken(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId), owner))
                .andExpect(status().isOk());
        long equipmentId = equipmentRepository.findByCoworkingSpaceId(spaceId).get(0).getId();
        String member = newMember();
        long id = json(book(member, booking(spaceId, 1, 1, ",\"equipmentIds\":[" + equipmentId + "]")), 200)
                .get("id").asLong();

        mockMvc.perform(withToken(delete("/api/reservations/{id}", id), member)).andExpect(status().isNoContent());
    }

    @Test
    void statusUpdateValidation() throws Exception {
        String admin = adminToken();
        mockMvc.perform(withToken(put("/api/reservations/{id}/status", 999_999), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(withToken(put("/api/reservations/{id}/status", 1), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"missing status\"}"))
                .andExpect(status().isBadRequest());
    }
}
