package com.coworkinghub.coworkinghub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class CoworkingSpace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double coordinateX;
    private double coordinateY;
    private String address;
    private byte[] images;

    @OneToMany(mappedBy = "coworkingSpace")
    @JsonIgnore
    private Set<Room> rooms;

    @OneToMany(mappedBy = "coworkingSpace")
    @JsonIgnore
    private Set<Equipment> equipments;

    // Set from the authenticated account on creation; exposed in JSON as ownerId only.
    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private Compte owner;

    @JsonProperty(value = "ownerId", access = JsonProperty.Access.READ_ONLY)
    public Long getOwnerId() {
        return owner == null ? null : owner.getId();
    }
}
