package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.model.Reservation;
import com.coworkinghub.coworkinghub.model.ReservationDto;
import com.coworkinghub.coworkinghub.model.ReservationStatusUpdateDto;
import com.coworkinghub.coworkinghub.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping({"", "/"})
    public ResponseEntity<Reservation> createReservation(@Valid @RequestBody ReservationDto reservationDto) {
        return ResponseEntity.ok(reservationService.createReservation(reservationDto));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<Reservation>> listReservations(@RequestParam(required = false) Long userId,
                                                              @RequestParam(required = false) Long coworkingSpaceId) {
        return ResponseEntity.ok(reservationService.find(userId, coworkingSpaceId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<Reservation>> myReservations() {
        return ResponseEntity.ok(reservationService.findMine());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Reservation> updateReservationStatus(@PathVariable Long id,
                                                               @Valid @RequestBody ReservationStatusUpdateDto statusUpdateDto) {
        Reservation updatedReservation = reservationService.updateReservationStatus(id,
                statusUpdateDto.getStatus(),
                statusUpdateDto.getReason());
        return ResponseEntity.ok(updatedReservation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
