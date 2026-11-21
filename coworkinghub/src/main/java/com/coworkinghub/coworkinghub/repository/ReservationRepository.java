package com.coworkinghub.coworkinghub.repository;

import com.coworkinghub.coworkinghub.model.Reservation;
import com.coworkinghub.coworkinghub.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// Paths use "_" so Spring Data does not mistake the JSON helpers getUserId()/getCoworkingSpaceId()
// on Reservation for entity attributes.
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCompte_Id(Long compteId);

    List<Reservation> findByCoworkingSpace_Id(Long coworkingSpaceId);

    List<Reservation> findByCompte_IdAndCoworkingSpace_Id(Long compteId, Long coworkingSpaceId);

    /** Reservations that still hold the room on that day (everything except declined ones). */
    List<Reservation> findByRoom_IdAndDateAndStatusNot(Long roomId, LocalDate date, ReservationStatus status);
}
