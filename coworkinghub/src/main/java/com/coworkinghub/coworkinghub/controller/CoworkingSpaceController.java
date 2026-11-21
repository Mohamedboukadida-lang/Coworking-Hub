package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.model.CoworkingSpace;
import com.coworkinghub.coworkinghub.service.CoworkingSpaceService;
import com.coworkinghub.coworkinghub.service.EquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coworking-spaces")
public class CoworkingSpaceController {

    private final CoworkingSpaceService coworkingSpaceService;
    private final EquipmentService equipmentService;

    public CoworkingSpaceController(CoworkingSpaceService coworkingSpaceService,
                                    EquipmentService equipmentService) {
        this.coworkingSpaceService = coworkingSpaceService;
        this.equipmentService = equipmentService;
    }

    // Both "" and "/" are mapped: Spring Boot 3 no longer matches trailing slashes automatically,
    // and existing clients call the "/" form.
    @PostMapping({"", "/"})
    public ResponseEntity<CoworkingSpace> createCoworkingSpace(@RequestBody CoworkingSpace coworkingSpace) {
        return ResponseEntity.ok(coworkingSpaceService.createCoworkingSpace(coworkingSpace));
    }

    @PostMapping("/{id}/add-random-equipment")
    public ResponseEntity<String> addRandomEquipment(@PathVariable Long id) {
        CoworkingSpace space = coworkingSpaceService.getById(id);
        equipmentService.addRandomEquipmentToCoworkingSpace(space);
        return ResponseEntity.ok("Random equipment added successfully to the coworking space with ID: " + id);
    }
}
