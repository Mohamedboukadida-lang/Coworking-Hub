package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.dto.CoworkingSpaceUpdateRequest;
import com.coworkinghub.coworkinghub.exception.ResourceNotFoundException;
import com.coworkinghub.coworkinghub.model.CoworkingSpace;
import com.coworkinghub.coworkinghub.model.Room;
import com.coworkinghub.coworkinghub.repository.CoworkingSpaceRepository;
import com.coworkinghub.coworkinghub.repository.RoomRepository;
import com.coworkinghub.coworkinghub.repository.UserRepository;
import com.coworkinghub.coworkinghub.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CoworkingSpaceService {

    private final CoworkingSpaceRepository coworkingSpaceRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public CoworkingSpaceService(CoworkingSpaceRepository coworkingSpaceRepository,
                                 RoomRepository roomRepository,
                                 UserRepository userRepository) {
        this.coworkingSpaceRepository = coworkingSpaceRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    /** Creates a space; the authenticated caller, if any, becomes its owner. */
    @Transactional
    public CoworkingSpace createCoworkingSpace(CoworkingSpace coworkingSpace) {
        coworkingSpace.setId(null);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(coworkingSpace::setOwner);
        return coworkingSpaceRepository.save(coworkingSpace);
    }

    @Transactional(readOnly = true)
    public List<CoworkingSpace> findAll() {
        return coworkingSpaceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CoworkingSpace getById(Long id) {
        return coworkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking space", id));
    }

    @Transactional
    public CoworkingSpace update(Long id, CoworkingSpaceUpdateRequest request) {
        CoworkingSpace space = getById(id);
        if (request.name() != null) space.setName(request.name());
        if (request.address() != null) space.setAddress(request.address());
        if (request.coordinateX() != null) space.setCoordinateX(request.coordinateX());
        if (request.coordinateY() != null) space.setCoordinateY(request.coordinateY());
        if (request.images() != null) space.setImages(request.images());
        return coworkingSpaceRepository.save(space);
    }

    /** Fails with 409 while rooms, equipment or reservations still reference the space. */
    @Transactional
    public void delete(Long id) {
        coworkingSpaceRepository.delete(getById(id));
        coworkingSpaceRepository.flush();
    }

    @Transactional
    public Room addRoom(Long spaceId, Room room) {
        room.setId(null);
        room.setCoworkingSpace(getById(spaceId));
        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<Room> findRooms(Long spaceId) {
        getById(spaceId);
        return roomRepository.findByCoworkingSpaceId(spaceId);
    }
}
