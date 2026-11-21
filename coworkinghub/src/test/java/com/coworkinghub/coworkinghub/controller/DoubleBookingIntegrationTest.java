package com.coworkinghub.coworkinghub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DoubleBookingIntegrationTest extends ApiTestSupport {

    @Test
    void overlappingBookingOfTheOnlyRoomIsRejected() throws Exception {
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(newMember(), booking(spaceId, "09:00", 3, 2, ""))).andExpect(status().isOk());

        // 10:00-11:00 is inside 09:00-12:00
        mockMvc.perform(book(newMember(), booking(spaceId, "10:00", 1, 1, "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("already booked")));
        // 08:00-10:00 overlaps the start
        mockMvc.perform(book(newMember(), booking(spaceId, "08:00", 2, 1, ""))).andExpect(status().isConflict());
    }

    @Test
    void backToBackBookingsAreAllowed() throws Exception {
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(newMember(), booking(spaceId, "09:00", 3, 2, ""))).andExpect(status().isOk());
        mockMvc.perform(book(newMember(), booking(spaceId, "12:00", 1, 1, ""))).andExpect(status().isOk());
        mockMvc.perform(book(newMember(), booking(spaceId, "07:00", 2, 1, ""))).andExpect(status().isOk());
    }

    @Test
    void sameSlotOnAnotherDayIsAllowed() throws Exception {
        long spaceId = createBookableSpace(newOwner());
        mockMvc.perform(book(newMember(), booking(spaceId, "09:00", 3, 2, ""))).andExpect(status().isOk());
        mockMvc.perform(book(newMember(), booking(spaceId, "09:00", 3, 2, "").replace(DATE, "2027-03-02")))
                .andExpect(status().isOk());
    }

    @Test
    void whenCheapestRoomIsTakenTheNextFreeFittingRoomIsUsed() throws Exception {
        String owner = newOwner();
        long spaceId = createSpace(owner);
        long cheap = addRoom(owner, spaceId, 4, 10.0);
        long pricier = addRoom(owner, spaceId, 4, 15.0);

        mockMvc.perform(book(newMember(), booking(spaceId, 2, 2, ""))).andExpect(jsonPath("$.roomId").value(cheap));
        mockMvc.perform(book(newMember(), booking(spaceId, 2, 2, "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(pricier))
                .andExpect(jsonPath("$.totalPrice").value(60.0));
        mockMvc.perform(book(newMember(), booking(spaceId, 2, 2, ""))).andExpect(status().isConflict());
    }

    @Test
    void declinedBookingFreesTheSlotAndCannotBeReactivatedOnceTaken() throws Exception {
        String owner = newOwner();
        long spaceId = createBookableSpace(owner);
        long first = json(book(newMember(), booking(spaceId, 2, 1, "")), 200).get("id").asLong();

        mockMvc.perform(withToken(put("/api/reservations/{id}/status", first), owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DECLINED\"}")).andExpect(status().isOk());
        mockMvc.perform(book(newMember(), booking(spaceId, 2, 1, ""))).andExpect(status().isOk());

        mockMvc.perform(withToken(put("/api/reservations/{id}/status", first), owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentRequestsForTheSameSlotBookItOnlyOnce() throws Exception {
        long spaceId = createBookableSpace(newOwner());
        int attempts = 8;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            tokens.add(newMember());
        }

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String token : tokens) {
            results.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(book(token, booking(spaceId, "14:00", 2, 1, "")))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(attempts - 1);
    }
}
