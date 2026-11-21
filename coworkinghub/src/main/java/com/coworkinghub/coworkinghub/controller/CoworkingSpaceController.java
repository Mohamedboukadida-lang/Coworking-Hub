package com.coworkinghub.coworkinghub.controller;

import com.coworkinghub.coworkinghub.dto.CoworkingSpaceUpdateRequest;
import com.coworkinghub.coworkinghub.model.CoworkingSpace;
import com.coworkinghub.coworkinghub.model.Equipment;
import com.coworkinghub.coworkinghub.model.Room;
import com.coworkinghub.coworkinghub.service.CoworkingSpaceService;
import com.coworkinghub.coworkinghub.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping({"", "/"})
    public ResponseEntity<List<CoworkingSpace>> listCoworkingSpaces() {
        return ResponseEntity.ok(coworkingSpaceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoworkingSpace> getCoworkingSpace(@PathVariable Long id) {
        return ResponseEntity.ok(coworkingSpaceService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CoworkingSpace> updateCoworkingSpace(@PathVariable Long id,
                                                               @RequestBody CoworkingSpaceUpdateRequest request) {
        return ResponseEntity.ok(coworkingSpaceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoworkingSpace(@PathVariable Long id) {
        coworkingSpaceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rooms")
    public ResponseEntity<Room> addRoom(@PathVariable Long id, @Valid @RequestBody Room room) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coworkingSpaceService.addRoom(id, room));
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<List<Room>> listRooms(@PathVariable Long id) {
        return ResponseEntity.ok(coworkingSpaceService.findRooms(id));
    }

    @GetMapping("/{id}/equipment")
    public ResponseEntity<List<Equipment>> listEquipment(@PathVariable Long id) {
        coworkingSpaceService.getById(id);
        return ResponseEntity.ok(equipmentService.findBySpace(id));
    }

    @PostMapping("/{id}/add-random-equipment")
    public ResponseEntity<String> addRandomEquipment(@PathVariable Long id) {
        CoworkingSpace space = coworkingSpaceService.getById(id);
        equipmentService.addRandomEquipmentToCoworkingSpace(space);
        return ResponseEntity.ok("Random equipment added successfully to the coworking space with ID: " + id);
    }
}
