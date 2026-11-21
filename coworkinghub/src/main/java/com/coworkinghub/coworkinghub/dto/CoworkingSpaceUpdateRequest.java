package com.coworkinghub.coworkinghub.dto;

/** Fields of a coworking space that can be changed. Null fields are left unchanged. */
public record CoworkingSpaceUpdateRequest(String name, String address, Double coordinateX,
                                          Double coordinateY, byte[] images) {
}
