package com.coworkinghub.coworkinghub.repository;

import com.coworkinghub.coworkinghub.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCoworkingSpace_Id(Long coworkingSpaceId);

    /** Rooms in the space that can hold at least {@code capacity} people, cheapest first. */
    List<Room> findByCoworkingSpace_IdAndCapacityGreaterThanEqualOrderByPricePerPersonPerHourAscIdAsc(
            Long coworkingSpaceId, int capacity);

    /**
     * Loads the room with a row lock (SELECT ... FOR UPDATE) held until the transaction ends, so two
     * bookings for the same room are checked for overlap one after the other, never at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> lockById(@Param("id") Long id);
}
