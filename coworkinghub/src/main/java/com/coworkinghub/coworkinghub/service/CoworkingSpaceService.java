package com.coworkinghub.coworkinghub.service;

import com.coworkinghub.coworkinghub.exception.ResourceNotFoundException;
import com.coworkinghub.coworkinghub.model.CoworkingSpace;
import com.coworkinghub.coworkinghub.repository.CoworkingSpaceRepository;
import org.springframework.stereotype.Service;

@Service
public class CoworkingSpaceService {

    private final CoworkingSpaceRepository coworkingSpaceRepository;

    public CoworkingSpaceService(CoworkingSpaceRepository coworkingSpaceRepository) {
        this.coworkingSpaceRepository = coworkingSpaceRepository;
    }

    public CoworkingSpace createCoworkingSpace(CoworkingSpace coworkingSpace) {
        return coworkingSpaceRepository.save(coworkingSpace);
    }

    public CoworkingSpace getById(Long id) {
        return coworkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coworking space", id));
    }
}
