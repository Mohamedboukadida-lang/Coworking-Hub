package com.coworkinghub.coworkinghub.repository;

import com.coworkinghub.coworkinghub.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
// Paths use "_" so Spring Data does not mistake the JSON helpers getUserId()/getCoworkingSpaceId()
// on Reservation for entity attributes.
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCompte_Id(Long compteId);

    List<Reservation> findByCoworkingSpace_Id(Long coworkingSpaceId);

    List<Reservation> findByCompte_IdAndCoworkingSpace_Id(Long compteId, Long coworkingSpaceId);
}
