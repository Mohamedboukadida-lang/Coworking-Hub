package com.coworkinghub.coworkinghub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ManyToOne
    @JoinColumn(name = "compte_id")
    @JsonIgnore
    private Compte compte;

    @OneToMany(mappedBy = "coworkingSpace")
    @JsonIgnore
    private Set<Room> rooms;

    @OneToMany(mappedBy = "coworkingSpace")
    @JsonIgnore
    private Set<Equipment> equipments;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Compte owner;
}
