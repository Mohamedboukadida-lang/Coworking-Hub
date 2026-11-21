package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.model.Reservation;
import com.coworkinghub.coworkinghub.model.ReservationDto;
import com.coworkinghub.coworkinghub.model.ReservationStatusUpdateDto;
import com.coworkinghub.coworkinghub.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{id}/status")
    public ResponseEntity<Reservation> updateReservationStatus(@PathVariable Long id,
                                                               @Valid @RequestBody ReservationStatusUpdateDto statusUpdateDto) {
        Reservation updatedReservation = reservationService.updateReservationStatus(id,
                statusUpdateDto.getStatus(),
                statusUpdateDto.getReason());
        return ResponseEntity.ok(updatedReservation);
    }
}
