package com.coworkinghub.coworkinghub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who may change or see what. */
@SpringBootTest
@AutoConfigureMockMvc
class OwnershipIntegrationTest extends ApiTestSupport {

    // ---- anonymous callers ----

    @Test
    void anonymousCallersCannotChangeAnything() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        long reservationId = json(book(newMember(), booking(spaceId, 1, 1, "")), 200).get("id").asLong();

        mockMvc.perform(postJson("/api/users/", "{\"name\":\"Anon\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(postJson("/api/coworking-spaces/", "{\"name\":\"Anon\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(postJson("/api/reservations/", booking(spaceId, 1, 1, ""))).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/reservations/{id}/status", reservationId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservations/{id}", reservationId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/{id}", idOf(owner))).andExpect(status().isUnauthorized());
    }

    // ---- accounts ----

    @Test
    void onlyAdminsCanUseTheAccountCreationEndpoint() throws Exception {
        mockMvc.perform(withToken(postJson("/api/users/",
                        "{\"name\":\"Mallory\",\"email\":\"%s\",\"password\":\"x-pass-123\",\"typeCompte\":\"ADMIN\"}"
                                .formatted(uniqueEmail())), newMember()))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersCannotEditOtherProfilesButAdminsCan() throws Exception {
        long victim = idOf(newMember());
        mockMvc.perform(withToken(put("/api/users/{id}", victim), newMember()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/api/users/{id}", victim), adminToken()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fixed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fixed"));
    }

    @Test
    void otherUsersSeeProfileWithoutEmail() throws Exception {
        long other = idOf(newMember());
        mockMvc.perform(withToken(get("/api/users/{id}", other), newMember()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amira"))
                .andExpect(jsonPath("$.email").doesNotExist());
        mockMvc.perform(withToken(get("/api/users/{id}", other), adminToken()))
                .andExpect(jsonPath("$.email").isString());
    }

    // ---- spaces ----

    @Test
    void membersCannotCreateSpaces() throws Exception {
        mockMvc.perform(withToken(postJson("/api/coworking-spaces/", "{\"name\":\"Nope\"}"), newMember()))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyTheOwnerOrAdminCanManageASpace() throws Exception {
        String owner = newOwner();
        long spaceId = createSpace(owner);
        String otherOwner = newOwner();

        mockMvc.perform(withToken(put("/api/coworking-spaces/{id}", spaceId), otherOwner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Stolen\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(postJson("/api/coworking-spaces/{id}/rooms",
                        "{\"capacity\":2,\"pricePerPersonPerHour\":1}", spaceId), otherOwner))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/api/coworking-spaces/{id}/add-random-equipment", spaceId), otherOwner))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(delete("/api/coworking-spaces/{id}", spaceId), otherOwner))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/coworking-spaces/{id}", spaceId)).andExpect(jsonPath("$.name").value("Hub Lac"));

        mockMvc.perform(withToken(put("/api/coworking-spaces/{id}", spaceId), adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Admin edit\"}"))
                .andExpect(status().isOk());
    }

    // ---- reservations ----

    @Test
    void reservationIsVisibleToBookerSpaceOwnerAndAdminOnly() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        String member = newMember();
        long id = json(book(member, booking(spaceId, 1, 1, "")), 200).get("id").asLong();

        mockMvc.perform(withToken(get("/api/reservations/{id}", id), member)).andExpect(status().isOk());
        mockMvc.perform(withToken(get("/api/reservations/{id}", id), owner)).andExpect(status().isOk());
        mockMvc.perform(withToken(get("/api/reservations/{id}", id), adminToken())).andExpect(status().isOk());
        mockMvc.perform(withToken(get("/api/reservations/{id}", id), newMember())).andExpect(status().isForbidden());
        mockMvc.perform(withToken(get("/api/reservations/{id}", id), newOwner())).andExpect(status().isForbidden());
    }

    @Test
    void onlyTheSpaceOwnerOrAdminCanAcceptOrDecline() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        String member = newMember();
        long id = json(book(member, booking(spaceId, 1, 1, "")), 200).get("id").asLong();
        String accept = "{\"status\":\"ACCEPTED\"}";

        mockMvc.perform(withToken(put("/api/reservations/{id}/status", id), member)
                .contentType(MediaType.APPLICATION_JSON).content(accept)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/api/reservations/{id}/status", id), newOwner())
                .contentType(MediaType.APPLICATION_JSON).content(accept)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(put("/api/reservations/{id}/status", id), owner)
                .contentType(MediaType.APPLICATION_JSON).content(accept)).andExpect(status().isOk());
    }

    @Test
    void onlyBookerSpaceOwnerOrAdminCanCancel() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        long first = json(book(newMember(), booking(spaceId, "08:00", 1, 1, "")), 200).get("id").asLong();
        long second = json(book(newMember(), booking(spaceId, "10:00", 1, 1, "")), 200).get("id").asLong();

        mockMvc.perform(withToken(delete("/api/reservations/{id}", first), newMember())).andExpect(status().isForbidden());
        mockMvc.perform(withToken(delete("/api/reservations/{id}", first), owner)).andExpect(status().isNoContent());
        mockMvc.perform(withToken(delete("/api/reservations/{id}", second), adminToken())).andExpect(status().isNoContent());
    }

    @Test
    void listingIsLimitedToOwnReservationsOrOwnSpaces() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        String member = newMember();
        long memberId = idOf(member);
        mockMvc.perform(book(member, booking(spaceId, 1, 1, ""))).andExpect(status().isOk());
        mockMvc.perform(book(member, booking(spaceId, "11:00", 1, 1, ""))).andExpect(status().isOk());
        String stranger = newMember();

        // no filter: your own
        mockMvc.perform(withToken(get("/api/reservations"), member)).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(withToken(get("/api/reservations"), stranger)).andExpect(jsonPath("$.length()").value(0));
        // someone else's
        mockMvc.perform(withToken(get("/api/reservations").param("userId", String.valueOf(memberId)), stranger))
                .andExpect(status().isForbidden());
        // a space you don't own: only your own bookings in it
        mockMvc.perform(withToken(get("/api/reservations").param("coworkingSpaceId", String.valueOf(spaceId)), stranger))
                .andExpect(jsonPath("$.length()").value(0));
        // a space you own: all of them
        mockMvc.perform(withToken(get("/api/reservations").param("coworkingSpaceId", String.valueOf(spaceId)), owner))
                .andExpect(jsonPath("$.length()").value(2));
        // admin: anyone's
        mockMvc.perform(withToken(get("/api/reservations").param("userId", String.valueOf(memberId)), adminToken()))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
