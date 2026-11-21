package com.coworkinghub.coworkinghub.repository;

import com.coworkinghub.coworkinghub.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByCoworkingSpaceId(Long coworkingSpaceId);
}
