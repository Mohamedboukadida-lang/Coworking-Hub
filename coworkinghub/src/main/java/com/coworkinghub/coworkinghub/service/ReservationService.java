package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.exception.ResourceNotFoundException;
import com.coworkinghub.coworkinghub.model.*;
import com.coworkinghub.coworkinghub.repository.CoworkingSpaceRepository;
import com.coworkinghub.coworkinghub.repository.EquipmentRepository;
import com.coworkinghub.coworkinghub.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CoworkingSpaceRepository coworkingSpaceRepository;
    private final EquipmentRepository equipmentRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              CoworkingSpaceRepository coworkingSpaceRepository,
                              EquipmentRepository equipmentRepository) {
        this.reservationRepository = reservationRepository;
        this.coworkingSpaceRepository = coworkingSpaceRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Transactional
    public Reservation createReservation(ReservationDto reservationDto) {
        Long spaceId = reservationDto.getCoworkingSpaceId();
        CoworkingSpace space = coworkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking space", spaceId));

        // TODO: availability checks (overlapping reservations, capacity) are not implemented yet.

        Reservation reservation = new Reservation();
        reservation.setCoworkingSpace(space);
        reservation.setDate(reservationDto.getDate());
        reservation.setTime(reservationDto.getTime());
        reservation.setHours(reservationDto.getHours());
        reservation.setNumberOfPeople(reservationDto.getNumberOfPeople());
        reservation.setStatus(ReservationStatus.PENDING);

        Set<Equipment> reservedEquipments = new HashSet<>();
        if (reservationDto.getEquipmentIds() != null) {
            for (Long equipmentId : reservationDto.getEquipmentIds()) {
                reservedEquipments.add(equipmentRepository.findById(equipmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Equipment", equipmentId)));
            }
        }
        reservation.setReservedEquipments(reservedEquipments);

        // TODO: totalPrice is not calculated yet; the pricing rule has not been defined.

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation updateReservationStatus(Long reservationId, ReservationStatus newStatus, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        reservation.setStatus(newStatus);
        reservation.setStatusChangeReason(reason);

        return reservationRepository.save(reservation);
    }
}
