package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.model.CoworkingSpace;
import com.coworkinghub.coworkinghub.model.Equipment;
import com.coworkinghub.coworkinghub.model.EquipmentType;
import com.coworkinghub.coworkinghub.repository.EquipmentRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    /** Adds one randomly named and priced item of every {@link EquipmentType} to the space. */
    @Transactional
    public List<Equipment> addRandomEquipmentToCoworkingSpace(CoworkingSpace coworkingSpace) {
        List<Equipment> equipments = new ArrayList<>();
        for (EquipmentType type : EquipmentType.values()) {
            Equipment equipment = new Equipment();
            equipment.setName(type.name() + " " + RandomStringUtils.insecure().nextAlphabetic(5));
            equipment.setPrice(Math.round(ThreadLocalRandom.current().nextDouble(100) * 100) / 100.0);
            equipment.setTypeOfEquipment(type);
            equipment.setCoworkingSpace(coworkingSpace);
            equipments.add(equipment);
        }
        return equipmentRepository.saveAll(equipments);
    }

    @Transactional(readOnly = true)
    public List<Equipment> findBySpace(Long spaceId) {
        return equipmentRepository.findByCoworkingSpaceId(spaceId);
    }
}
