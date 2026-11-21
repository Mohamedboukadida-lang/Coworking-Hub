package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.exception.ApiException;
import com.coworkinghub.coworkinghub.exception.ResourceNotFoundException;
import com.coworkinghub.coworkinghub.model.*;
import com.coworkinghub.coworkinghub.repository.CoworkingSpaceRepository;
import com.coworkinghub.coworkinghub.repository.EquipmentRepository;
import com.coworkinghub.coworkinghub.repository.ReservationRepository;
import com.coworkinghub.coworkinghub.repository.RoomRepository;
import com.coworkinghub.coworkinghub.repository.UserRepository;
import com.coworkinghub.coworkinghub.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CoworkingSpaceRepository coworkingSpaceRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              CoworkingSpaceRepository coworkingSpaceRepository,
                              EquipmentRepository equipmentRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.coworkingSpaceRepository = coworkingSpaceRepository;
        this.equipmentRepository = equipmentRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a PENDING reservation in the cheapest room of the space that fits the party, priced as
     * {@code (room price per person per hour x people + sum of equipment prices) x hours}.
     * The authenticated caller, if any, becomes the reservation's user.
     */
    @Transactional
    public Reservation createReservation(ReservationDto reservationDto) {
        Long spaceId = reservationDto.getCoworkingSpaceId();
        CoworkingSpace space = coworkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking space", spaceId));

        int people = reservationDto.getNumberOfPeople();
        int hours = reservationDto.getHours();
        Room room = roomRepository
                .findFirstByCoworkingSpaceIdAndCapacityGreaterThanEqualOrderByPricePerPersonPerHourAsc(spaceId, people)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "No room in coworking space " + spaceId + " can hold " + people + " people"));

        Set<Equipment> reservedEquipments = new HashSet<>();
        if (reservationDto.getEquipmentIds() != null) {
            for (Long equipmentId : reservationDto.getEquipmentIds()) {
                Equipment equipment = equipmentRepository.findById(equipmentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Equipment", equipmentId));
                if (equipment.getCoworkingSpace() == null
                        || !Objects.equals(equipment.getCoworkingSpace().getId(), spaceId)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Equipment " + equipmentId + " does not belong to coworking space " + spaceId);
                }
                reservedEquipments.add(equipment);
            }
        }

        // TODO: availability checks (overlapping reservations for the same room) are not implemented yet.

        Reservation reservation = new Reservation();
        reservation.setCoworkingSpace(space);
        reservation.setRoom(room);
        reservation.setDate(reservationDto.getDate());
        reservation.setTime(reservationDto.getTime());
        reservation.setHours(hours);
        reservation.setNumberOfPeople(people);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setReservedEquipments(reservedEquipments);
        reservation.setTotalPrice(calculateTotalPrice(room, reservedEquipments, people, hours));
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(reservation::setCompte);

        return reservationRepository.save(reservation);
    }

    static double calculateTotalPrice(Room room, Set<Equipment> equipments, int people, int hours) {
        double equipmentPerHour = equipments.stream().mapToDouble(Equipment::getPrice).sum();
        double total = (room.getPricePerPersonPerHour() * people + equipmentPerHour) * hours;
        return Math.round(total * 100) / 100.0;
    }

    @Transactional
    public Reservation updateReservationStatus(Long reservationId, ReservationStatus newStatus, String reason) {
        Reservation reservation = getById(reservationId);
        reservation.setStatus(newStatus);
        reservation.setStatusChangeReason(reason);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Reservation getById(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }

    /** Lists reservations, optionally filtered by user and/or coworking space. */
    @Transactional(readOnly = true)
    public List<Reservation> find(Long userId, Long coworkingSpaceId) {
        if (userId != null && coworkingSpaceId != null) {
            return reservationRepository.findByCompte_IdAndCoworkingSpace_Id(userId, coworkingSpaceId);
        }
        if (userId != null) {
            return reservationRepository.findByCompte_Id(userId);
        }
        if (coworkingSpaceId != null) {
            return reservationRepository.findByCoworkingSpace_Id(coworkingSpaceId);
        }
        return reservationRepository.findAll();
    }

    @Transactional
    public void delete(Long id) {
        reservationRepository.delete(getById(id));
    }
}
