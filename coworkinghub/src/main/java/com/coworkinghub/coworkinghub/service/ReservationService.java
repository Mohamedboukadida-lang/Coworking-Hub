package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.exception.ApiException;
import com.coworkinghub.coworkinghub.exception.ResourceNotFoundException;
import com.coworkinghub.coworkinghub.model.*;
import com.coworkinghub.coworkinghub.repository.CoworkingSpaceRepository;
import com.coworkinghub.coworkinghub.repository.EquipmentRepository;
import com.coworkinghub.coworkinghub.repository.ReservationRepository;
import com.coworkinghub.coworkinghub.repository.RoomRepository;
import com.coworkinghub.coworkinghub.security.AccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.coworkinghub.coworkinghub.security.AccessService.*;

@Service
public class ReservationService {

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final DateTimeFormatter TIME_INPUT = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter TIME_STORED = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationRepository reservationRepository;
    private final CoworkingSpaceRepository coworkingSpaceRepository;
    private final EquipmentRepository equipmentRepository;
    private final RoomRepository roomRepository;
    private final AccessService accessService;

    public ReservationService(ReservationRepository reservationRepository,
                              CoworkingSpaceRepository coworkingSpaceRepository,
                              EquipmentRepository equipmentRepository,
                              RoomRepository roomRepository,
                              AccessService accessService) {
        this.reservationRepository = reservationRepository;
        this.coworkingSpaceRepository = coworkingSpaceRepository;
        this.equipmentRepository = equipmentRepository;
        this.roomRepository = roomRepository;
        this.accessService = accessService;
    }

    /**
     * Books the cheapest room of the space that fits the party and is free for the whole slot, priced as
     * {@code (room price per person per hour x people + sum of equipment prices) x hours}.
     * 400 if no room is big enough, 409 if every big-enough room is already taken for that slot.
     */
    @Transactional
    public Reservation createReservation(ReservationDto dto) {
        Compte caller = accessService.caller();
        Long spaceId = dto.getCoworkingSpaceId();
        CoworkingSpace space = coworkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking space", spaceId));

        int people = dto.getNumberOfPeople();
        int hours = dto.getHours();
        LocalTime start = LocalTime.parse(dto.getTime(), TIME_INPUT);
        int startMinute = start.getHour() * 60 + start.getMinute();
        int endMinute = startMinute + hours * 60;
        if (endMinute > MINUTES_PER_DAY) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A reservation must end by midnight on the same day");
        }

        Set<Equipment> reservedEquipments = loadEquipment(dto.getEquipmentIds(), spaceId);

        List<Room> candidates = roomRepository
                .findByCoworkingSpace_IdAndCapacityGreaterThanEqualOrderByPricePerPersonPerHourAscIdAsc(spaceId, people);
        if (candidates.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No room in coworking space " + spaceId + " can hold " + people + " people");
        }
        Room room = null;
        for (Room candidate : candidates) {
            // The lock is held until commit, so a concurrent booking of this room waits and then sees ours.
            Room locked = roomRepository.lockById(candidate.getId()).orElseThrow();
            if (!overlapsExisting(locked.getId(), dto.getDate(), startMinute, endMinute, null)) {
                room = locked;
                break;
            }
        }
        if (room == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Every room that fits " + people + " people in coworking space "
                    + spaceId + " is already booked on " + dto.getDate() + " between " + start.format(TIME_STORED)
                    + " and " + formatMinute(endMinute));
        }

        Reservation reservation = new Reservation();
        reservation.setCoworkingSpace(space);
        reservation.setRoom(room);
        reservation.setCompte(caller);
        reservation.setDate(dto.getDate());
        reservation.setTime(start.format(TIME_STORED));
        reservation.setHours(hours);
        reservation.setNumberOfPeople(people);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setReservedEquipments(reservedEquipments);
        reservation.setTotalPrice(calculateTotalPrice(room, reservedEquipments, people, hours));
        return reservationRepository.save(reservation);
    }

    private Set<Equipment> loadEquipment(Set<Long> equipmentIds, Long spaceId) {
        Set<Equipment> equipments = new HashSet<>();
        if (equipmentIds == null) {
            return equipments;
        }
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Equipment", equipmentId));
            if (equipment.getCoworkingSpace() == null
                    || !Objects.equals(equipment.getCoworkingSpace().getId(), spaceId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Equipment " + equipmentId + " does not belong to coworking space " + spaceId);
            }
            equipments.add(equipment);
        }
        return equipments;
    }

    /** True if a non-declined reservation of the room on that day overlaps [start, end) in minutes. */
    private boolean overlapsExisting(Long roomId, LocalDate date, int start, int end, Long ignoreReservationId) {
        for (Reservation other : reservationRepository.findByRoom_IdAndDateAndStatusNot(roomId, date,
                ReservationStatus.DECLINED)) {
            if (Objects.equals(other.getId(), ignoreReservationId)) {
                continue;
            }
            int otherStart = startMinute(other);
            int otherEnd = otherStart + other.getHours() * 60;
            if (start < otherEnd && otherStart < end) {
                return true;
            }
        }
        return false;
    }

    private static int startMinute(Reservation reservation) {
        LocalTime t = LocalTime.parse(reservation.getTime(), TIME_INPUT);
        return t.getHour() * 60 + t.getMinute();
    }

    private static String formatMinute(int minute) {
        return minute == MINUTES_PER_DAY ? "24:00" : LocalTime.of(minute / 60, minute % 60).format(TIME_STORED);
    }

    static double calculateTotalPrice(Room room, Set<Equipment> equipments, int people, int hours) {
        double equipmentPerHour = equipments.stream().mapToDouble(Equipment::getPrice).sum();
        double total = (room.getPricePerPersonPerHour() * people + equipmentPerHour) * hours;
        return Math.round(total * 100) / 100.0;
    }

    /** Space owner or admin. Re-activating a declined booking is refused if its slot was taken meanwhile. */
    @Transactional
    public Reservation updateReservationStatus(Long reservationId, ReservationStatus newStatus, String reason) {
        Reservation reservation = findOrThrow(reservationId);
        accessService.requireCanManage(reservation.getCoworkingSpace());

        boolean reactivating = reservation.getStatus() == ReservationStatus.DECLINED
                && newStatus != ReservationStatus.DECLINED;
        if (reactivating && reservation.getRoom() != null && reservation.getDate() != null
                && reservation.getTime() != null) {
            Room room = roomRepository.lockById(reservation.getRoom().getId()).orElseThrow();
            int start = startMinute(reservation);
            if (overlapsExisting(room.getId(), reservation.getDate(), start, start + reservation.getHours() * 60,
                    reservation.getId())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "The room has been booked by someone else for this slot since the reservation was declined");
            }
        }

        reservation.setStatus(newStatus);
        reservation.setStatusChangeReason(reason);
        return reservationRepository.save(reservation);
    }

    /** Visible to the person who booked it, the space owner and admins. */
    @Transactional(readOnly = true)
    public Reservation getById(Long id) {
        Reservation reservation = findOrThrow(id);
        Compte caller = accessService.caller();
        require(isAdmin(caller) || booked(caller, reservation) || owns(caller, reservation.getCoworkingSpace()),
                "You cannot see this reservation");
        return reservation;
    }

    /**
     * Admins may list anything. Others see their own reservations, optionally within one space, or all
     * reservations of a space they own.
     */
    @Transactional(readOnly = true)
    public List<Reservation> find(Long userId, Long coworkingSpaceId) {
        Compte caller = accessService.caller();
        if (!isAdmin(caller)) {
            boolean ownSpaceListing = userId == null && coworkingSpaceId != null
                    && coworkingSpaceRepository.findById(coworkingSpaceId).map(s -> owns(caller, s)).orElse(false);
            if (!ownSpaceListing) {
                require(userId == null || Objects.equals(userId, caller.getId()),
                        "You can only list your own reservations or those of spaces you own");
                userId = caller.getId();
            }
        }
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

    /** The caller's own reservations. */
    @Transactional(readOnly = true)
    public List<Reservation> findMine() {
        return reservationRepository.findByCompte_Id(accessService.caller().getId());
    }

    /** The person who booked it, the space owner or an admin may cancel. */
    @Transactional
    public void delete(Long id) {
        Reservation reservation = findOrThrow(id);
        Compte caller = accessService.caller();
        require(isAdmin(caller) || booked(caller, reservation) || owns(caller, reservation.getCoworkingSpace()),
                "You cannot cancel this reservation");
        reservationRepository.delete(reservation);
    }

    private Reservation findOrThrow(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }
}
