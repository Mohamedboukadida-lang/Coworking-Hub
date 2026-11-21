package com.coworkinghub.coworkinghub.repository;

import com.coworkinghub.coworkinghub.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCoworkingSpaceId(Long coworkingSpaceId);

    /** Cheapest room in the space that can hold at least {@code capacity} people. */
    Optional<Room> findFirstByCoworkingSpaceIdAndCapacityGreaterThanEqualOrderByPricePerPersonPerHourAsc(
            Long coworkingSpaceId, int capacity);
}
